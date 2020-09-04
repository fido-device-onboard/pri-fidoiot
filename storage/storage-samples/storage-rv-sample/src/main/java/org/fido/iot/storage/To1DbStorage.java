// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.InvalidJwtException;
import org.fido.iot.protocol.ResourceNotFoundException;
import org.fido.iot.protocol.To1ServerStorage;

/**
 * To1 Database Storage implementation.
 */
public class To1DbStorage implements To1ServerStorage {

  private final CryptoService cryptoService;
  private final DataSource dataSource;
  private byte[] nonce4;
  private UUID guid;

  public To1DbStorage(CryptoService cryptoService, DataSource dataSource) {
    this.cryptoService = cryptoService;
    this.dataSource = dataSource;
  }

  @Override
  public byte[] getNonce4() {
    return nonce4;
  }

  @Override
  public void setNonce4(byte[] nonce4) {
    this.nonce4 = nonce4;
  }

  @Override
  public void setGuid(UUID guid) {
    this.guid = guid;
  }

  @Override
  public UUID getGuid() {
    return guid;
  }

  @Override
  public Composite getSigInfoB(Composite signInfoA) {
    return signInfoA; //for ecdsa we just echo
  }

  @Override
  public PublicKey getVerificationKey() {
    String sql = "SELECT DEVICE_KEY FROM RV_REDIRECTS WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, getGuid().toString());

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          return getCryptoService()
              .decode(Composite.fromObject(rs.getBytes(1)));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    throw new ResourceNotFoundException(getGuid().toString());
  }

  @Override
  public Composite getRedirectBlob() {
    String sql = "SELECT REDIRECT_BLOB FROM RV_REDIRECTS WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, getGuid().toString());

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          return Composite.fromObject(rs.getBinaryStream(1));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    throw new ResourceNotFoundException(getGuid().toString());
  }

  @Override
  public void starting(Composite request, Composite reply) {

  }

  @Override
  public void started(Composite request, Composite reply) {

    String sessionId = UUID.randomUUID().toString();
    reply.set(Const.SM_PROTOCOL_INFO,
        Composite.newMap().set(Const.PI_TOKEN, sessionId));

    String sql = "INSERT INTO TO1_SESSIONS "
        + "(SESSION_ID,GUID,NONCE,CREATED) VALUES (?,?,?,?);";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, sessionId);
      pstmt.setString(2, getGuid().toString());
      pstmt.setBytes(3, nonce4);
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(4, created);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void continuing(Composite request, Composite reply) {
    String token = getToken(request);

    String sql = "SELECT GUID, NONCE, FROM TO1_SESSIONS WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, token);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          guid = UUID.fromString(rs.getString(1));
          nonce4 = rs.getBytes(2);
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    if (nonce4 == null) {
      throw new InvalidJwtException(token);
    }

  }

  @Override
  public void continued(Composite request, Composite reply) {

  }

  @Override
  public void completed(Composite request, Composite reply) {
    String token = getToken(request);
    String sql = "DELETE FROM TO1_SESSIONS WHERE SESSION_ID = ?;";

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
