// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.InvalidJwtException;
import org.fido.iot.protocol.KeyResolver;
import org.fido.iot.protocol.ResourceNotFoundException;
import org.fido.iot.protocol.ServiceInfoEncoder;
import org.fido.iot.protocol.To2ServerStorage;

public class OwnerDbStorage implements To2ServerStorage {

  private final CryptoService cryptoService;
  private final DataSource dataSource;
  private final KeyResolver keyResolver;
  private Composite ownerState;
  private UUID guid;
  private String cipherName;
  private byte[] nonce6;
  private byte[] nonce7;
  private Composite voucher;
  private String sessionId;

  /**
   * Constructs a OwnerDbStorage instance.
   *
   * @param cs          A cryptoService.
   * @param ds          A SQL datasource.
   * @param keyResolver A key resolver.
   */
  public OwnerDbStorage(CryptoService cs, DataSource ds, KeyResolver keyResolver) {
    cryptoService = cs;
    dataSource = ds;
    this.keyResolver = keyResolver;
  }

  @Override
  public PrivateKey geOwnerSigningKey(PublicKey key) {
    return keyResolver.getKey(key);
  }

  @Override
  public byte[] getNonce6() {
    return nonce6;
  }

  @Override
  public void setNonce6(byte[] nonce) {
    nonce6 = nonce;
  }

  @Override
  public void setOwnerState(Composite ownerState) {
    this.ownerState = ownerState;
  }

  @Override
  public Composite getOwnerState() {
    return ownerState;
  }

  @Override
  public void setCipherName(String cipherName) {
    this.cipherName = cipherName;
  }

  @Override
  public String getCipherName() {
    return cipherName;
  }

  @Override
  public void setGuid(UUID guid) {
    this.guid = guid;
    voucher = getVoucher();
  }

  @Override
  public Composite getVoucher() {
    if (voucher == null) {
      String sql = "SELECT VOUCHER FROM TO2_DEVICES WHERE GUID = ?;";

      try (Connection conn = dataSource.getConnection();
          PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, guid.toString());
        try (ResultSet rs = pstmt.executeQuery()) {
          while (rs.next()) {
            voucher = Composite.fromObject(rs.getBinaryStream(1));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    if (voucher == null) {
      throw new ResourceNotFoundException(guid.toString());
    }
    return voucher;
  }

  @Override
  public void setNonce7(byte[] nonce7) {
    this.nonce7 = nonce7;
    String sql = "UPDATE TO2_SESSIONS "
        + "SET NONCE7 = ?,"
        + "UPDATED = ? "
        + "WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, nonce7);
      Timestamp updatedAt = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(2, updatedAt);
      pstmt.setString(3, sessionId);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public byte[] getNonce7() {
    return nonce7;
  }

  @Override
  public Composite getSigInfoB(Composite sigInfoA) {
    return sigInfoA;
  }

  @Override
  public Composite getReplacementRvInfo() {

    return voucher
        .getAsComposite(Const.OV_HEADER)
        .getAsComposite(Const.OVH_RENDEZVOUS_INFO);
  }

  @Override
  public UUID getReplacementGuid() {
    return voucher
        .getAsComposite(Const.OV_HEADER)
        .getAsUuid(Const.OVH_GUID);
  }

  @Override
  public Composite getReplacementOwnerKey() {
    return getCryptoService().getOwnerPublicKey(voucher);
  }

  @Override
  public void continuing(Composite request, Composite reply) {
    sessionId = getToken(request);

    if (sessionId.isEmpty()) {
      throw new InvalidJwtException();
    }

    String sql = "SELECT VOUCHER, OWNER_STATE, CIPHER_NAME, NONCE6, NONCE7 "
        + "FROM TO2_SESSIONS WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, sessionId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          voucher = Composite.fromObject(rs.getBinaryStream(1));
          ownerState = Composite.fromObject(rs.getBinaryStream(2));
          cipherName = rs.getString(3);
          nonce6 = rs.getBytes(4);
          nonce7 = rs.getBytes(5);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    if (voucher == null) {
      throw new InvalidJwtException(sessionId);
    }

  }

  @Override
  public Composite getNextServiceInfo() {
    //should come from database
    return ServiceInfoEncoder.encodeOwnerServiceInfo(
        Collections.EMPTY_LIST, false, true);
  }

  @Override
  public void setServiceInfo(Composite info, boolean isMore) {

  }

  @Override
  public void setReplacementHmac(Composite hmac) {

  }

  @Override
  public void prepareServiceInfo() {

  }

  @Override
  public void starting(Composite request, Composite reply) {

  }

  @Override
  public void started(Composite request, Composite reply) {
    String sessionId = UUID.randomUUID().toString();
    reply.set(Const.SM_PROTOCOL_INFO,
        Composite.newMap().set(Const.PI_TOKEN, sessionId));

    String sql = "INSERT INTO TO2_SESSIONS "
        + "(SESSION_ID,"
        + "VOUCHER,"
        + "OWNER_STATE,"
        + "CIPHER_NAME, "
        + "NONCE6,"
        + "NONCE7,"
        + "CREATED,"
        + "UPDATED) "
        + "VALUES (?,?,?,?,?,?,?,?);";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, sessionId);
      pstmt.setBytes(2, voucher.toBytes());
      pstmt.setBytes(3, ownerState.toBytes());
      pstmt.setString(4, cipherName);
      pstmt.setBytes(5, nonce6);
      pstmt.setBytes(6, nonce7);
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(7, created);
      pstmt.setTimestamp(8, created);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void completed(Composite request, Composite reply) {

    String sql = "DELETE FROM TO2_SESSIONS WHERE SESSION_ID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, sessionId);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public void failed(Composite request, Composite reply) {

  }

  @Override
  public void continued(Composite request, Composite reply) {
    String sql = "UPDATE TO2_SESSIONS "
        + "SET OWNER_STATE = ? ,"
        + "UPDATED = ? "
        + "WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setBytes(1, ownerState.toBytes());
      Timestamp updatedAt = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(2, updatedAt);
      pstmt.setString(3, sessionId);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private CryptoService getCryptoService() {
    return cryptoService;
  }

  protected static String getToken(Composite request) {
    Composite protocolInfo = request.getAsComposite(Const.SM_PROTOCOL_INFO);
    if (!protocolInfo.containsKey(Const.PI_TOKEN)) {
      throw new InvalidJwtException();
    }
    return protocolInfo.getAsString(Const.PI_TOKEN);
  }
}
