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
import org.fidoalliance.fdo.protocol.To1ServerStorage;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;

/**
 * To1 Database Storage implementation.
 */
public class To1DbStorage implements To1ServerStorage {

  private final CryptoService cryptoService;
  private final DataSource dataSource;
  private final OnDieService onDieService;
  private byte[] nonceTo1Proof;
  private UUID guid;
  private Composite sigInfoA;

  /**
   * Constructor.
   *
   * @param cryptoService cryptoService used for crypto operations
   * @param dataSource database source connection
   * @param ods service object used for OnDie operations
   */
  public To1DbStorage(CryptoService cryptoService, DataSource dataSource, OnDieService ods) {
    this.cryptoService = cryptoService;
    this.dataSource = dataSource;
    this.onDieService = ods;
  }

  @Override
  public byte[] getNonceTo1Proof() {
    return nonceTo1Proof;
  }

  @Override
  public void setNonceTo1Proof(byte[] nonceTo1Proof) {
    this.nonceTo1Proof = nonceTo1Proof;
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
  public String getExpiryTimeStamp() {
    String sql = "SELECT EXPIRES_AT FROM RV_REDIRECTS WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, getGuid().toString());

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
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
        + "(SESSION_ID,GUID,NONCE,SIGINFOA,CREATED) VALUES (?,?,?,?,?);";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, sessionId);
      pstmt.setString(2, getGuid().toString());
      pstmt.setBytes(3, nonceTo1Proof);
      pstmt.setBytes(4, sigInfoA.toBytes());
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(5, created);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void continuing(Composite request, Composite reply) {
    String token = getToken(request);

    String sql = "SELECT GUID, NONCE, SIGINFOA FROM TO1_SESSIONS WHERE SESSION_ID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, token);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          guid = UUID.fromString(rs.getString(1));
          nonceTo1Proof = rs.getBytes(2);
          sigInfoA = Composite.fromObject(rs.getBinaryStream(3));
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    if (nonceTo1Proof == null) {
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
    completed(request, reply);
  }

  private CryptoService getCryptoService() {
    return cryptoService;
  }

  public OnDieService getOnDieService() {
    return onDieService;
  }

  protected static String getToken(Composite request) {
    Composite protocolInfo = request.getAsComposite(Const.SM_PROTOCOL_INFO);
    if (!protocolInfo.containsKey(Const.PI_TOKEN)) {
      throw new InvalidJwtException();
    }
    return protocolInfo.getAsString(Const.PI_TOKEN);
  }

  @Override
  public Composite getSigInfoA() {
    return sigInfoA;
  }

  @Override
  public void setSigInfoA(Composite sigInfoA) {
    this.sigInfoA = sigInfoA;
  }
}
