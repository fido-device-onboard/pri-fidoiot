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
import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.KeyResolver;
import org.fido.iot.protocol.To0ClientStorage;

public class OwnerDbTo0Storage implements To0ClientStorage {

  private DataSource dataSource;
  private KeyResolver keyResolver;
  private Composite voucher;
  private UUID guid;
  private String clientToken;

  // default TO0 waitseconds
  public static final int TO0_REQUEST_WS = 3600;

  /**
   * Constructor.
   * 
   * @param dataSource A SQL datasource
   * @param keyResolver Instance of {@link KeyResolver}
   * @param guid {@link UUID} of the device the storage will serve
   */
  public OwnerDbTo0Storage(DataSource dataSource, KeyResolver keyResolver, UUID guid) {
    this.dataSource = dataSource;
    this.keyResolver = keyResolver;
    this.guid = guid;
  }

  @Override
  public Composite getVoucher() {
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
    return voucher;
  }

  @Override
  public Composite getRedirectBlob() {
    return voucher.getAsComposite(Const.OV_HEADER).getAsComposite(Const.OVH_RENDEZVOUS_INFO);
  }

  @Override
  public long getRequestWait() {
    String sql = "SELECT WAIT_SECONDS_REQUEST FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
    // return default value
    return TO0_REQUEST_WS;
  }

  @Override
  public void setResponseWait(long wait) {
    String sql = "UPDATE TO2_DEVICES SET WAIT_SECONDS_RESPONSE = ? WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setInt(1, (int) wait);
      pstmt.setString(2, guid.toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    System.out.println("To0 Response Wait for " + guid.toString() + " : " + Long.toString(wait));
  }

  @Override
  public PrivateKey getOwnerSigningKey(PublicKey ownerPublicKey) {
    return keyResolver.getKey(ownerPublicKey);
  }

  @Override
  public void starting(Composite request, Composite reply) {

  }

  @Override
  public void started(Composite request, Composite reply) {
    String sql = "UPDATE TO2_DEVICES SET TO0_STARTED = ? WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(1, created);
      pstmt.setString(2, guid.toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void continuing(Composite request, Composite reply) {
    Composite info = request.getAsComposite(Const.SM_PROTOCOL_INFO);
    if (info.containsKey(Const.PI_TOKEN)) {
      clientToken = info.getAsString(Const.PI_TOKEN);
    }
    reply.set(Const.SM_PROTOCOL_INFO, Composite.newMap().set(Const.PI_TOKEN, clientToken));
  }

  @Override
  public void continued(Composite request, Composite reply) {

  }

  @Override
  public void completed(Composite request, Composite reply) {
    // log first since TO0 is done. Update DB later.
    System.out.println("TO0 Client finished for GUID " + guid.toString());
    String sql = "UPDATE TO2_DEVICES SET TO0_COMPLETED = ? WHERE GUID = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(1, created);
      pstmt.setString(2, guid.toString());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void failed(Composite request, Composite reply) {
    System.out.println("TO0 Client failed for GUID " + guid.toString());
  }
}
