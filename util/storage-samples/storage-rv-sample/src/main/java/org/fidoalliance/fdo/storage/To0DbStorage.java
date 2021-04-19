// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.storage;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;
import javax.sql.DataSource;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.InvalidJwtException;
import org.fidoalliance.fdo.protocol.ResourceNotFoundException;
import org.fidoalliance.fdo.protocol.To0ServerStorage;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;

/**
 * Database Storage implementation.
 */
public class To0DbStorage implements To0ServerStorage {

  private final CryptoService cryptoService;
  protected final DataSource dataSource;
  private byte[] nonceTo0Sign;

  /**
   * Constructs a To0DbStorage instance.
   *
   * @param cryptoService A crypto Service.
   * @param dataSource    A SQL datasource.
   */
  public To0DbStorage(CryptoService cryptoService, DataSource dataSource) {
    this.cryptoService = cryptoService;
    this.dataSource = dataSource;
  }

  @Override
  public byte[] getNonceTo0Sign() {
    return nonceTo0Sign;
  }

  @Override
  public void setNonceTo0Sign(byte[] nonceTo0Sign) {
    this.nonceTo0Sign = nonceTo0Sign;
  }

  @Override
  public long storeRedirectBlob(Composite voucher, long requestedWait, byte[] signedBlob) {
    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    UUID guid = ovh.getAsUuid(Const.OVH_GUID);

    Composite encodedKey = getCryptoService().getOwnerPublicKey(voucher);
    PublicKey pubKey = getCryptoService().decode(encodedKey);
    String ownerX509String = getCryptoService().getFingerPrint(pubKey);
    pubKey = getCryptoService().getDevicePublicKey(voucher);
    Composite deviceX509 = Composite.newArray();
    if (pubKey != null) {
      deviceX509 = getCryptoService().encode(pubKey, Const.PK_ENC_X509);
    }

    String sql = ""
        + "MERGE INTO RV_REDIRECTS  "
        + "KEY (GUID) "
        + "VALUES (?,?,?,?,?,?,?); ";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      pstmt.setBytes(2, signedBlob);
      pstmt.setString(3, ownerX509String);
      pstmt.setBytes(4, deviceX509.toBytes());
      pstmt.setInt(5, Long.valueOf(requestedWait).intValue());
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(6, created);
      Timestamp expiresAt = new Timestamp(
          Calendar.getInstance().getTimeInMillis() + requestedWait * 1000);
      pstmt.setTimestamp(7, expiresAt);

      pstmt.executeUpdate();

      sql = "SELECT WAIT_SECONDS_RESPONSE FROM RV_REDIRECTS WHERE GUID = ?";
      try (PreparedStatement pstmt2 = conn.prepareStatement(sql)) {

        pstmt2.setString(1, guid.toString());
        try (ResultSet rs = pstmt2.executeQuery()) {
          while (rs.next()) {
            return rs.getInt(1);
          }
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    throw new ResourceNotFoundException(guid.toString());
  }

  @Override
  public void continuing(Composite request, Composite reply) {

    String token = getToken(request);

    String sql = "SELECT NONCE FROM TO0_SESSIONS WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, token);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          nonceTo0Sign = rs.getBytes(1);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    if (nonceTo0Sign == null) {
      throw new InvalidJwtException(token);
    }

  }

  @Override
  public void continued(Composite request, Composite reply) {

  }

  @Override
  public void starting(Composite request, Composite reply) {

  }

  @Override
  public void started(Composite request, Composite reply) {

    String sessionId = UUID.randomUUID().toString();
    reply.set(Const.SM_PROTOCOL_INFO,
        Composite.newMap().set(Const.PI_TOKEN, sessionId));

    String sql = "INSERT INTO TO0_SESSIONS (SESSION_ID,NONCE,CREATED) VALUES (?,?,?);";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, sessionId);
      pstmt.setBytes(2, nonceTo0Sign);
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(3, created);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void completed(Composite request, Composite reply) {

    String token = getToken(request);

    String sql = "DELETE FROM TO0_SESSIONS WHERE SESSION_ID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, token);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void failed(Composite request, Composite reply) {

  }

  protected CryptoService getCryptoService() {
    return cryptoService;
  }

  protected String getToken(Composite request) {
    Composite protocolInfo = request.getAsComposite(Const.SM_PROTOCOL_INFO);
    if (!protocolInfo.containsKey(Const.PI_TOKEN)) {
      throw new InvalidJwtException();
    }
    return protocolInfo.getAsString(Const.PI_TOKEN);
  }

}
