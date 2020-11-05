// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;

/**
 * Owner Database Manager.
 */
public class OwnerDbManager {

  /**
   * Creates owner tables.
   *
   * @param dataSource A SQL datasource.
   */
  public void createTables(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {

      String sql = "CREATE TABLE IF NOT EXISTS "
          + "TO2_DEVICES("
          + "GUID CHAR(36) PRIMARY KEY, "
          + "VOUCHER BLOB, "
          + "WAIT_SECONDS_REQUEST INT NOT NULL DEFAULT 3600, "
          + "WAIT_SECONDS_RESPONSE INT NULL DEFAULT NULL, "
          + "CREATED TIMESTAMP NOT NULL, "
          + "TO0_STARTED TIMESTAMP NULL DEFAULT NULL, "
          + "TO0_COMPLETED TIMESTAMP NULL DEFAULT NULL, "
          + "TO2_STARTED TIMESTAMP NULL DEFAULT NULL, "
          + "TO2_COMPLETED TIMESTAMP NULL DEFAULT NULL, "
          + "REPLACEMENT_GUID CHAR(36), "
          + "REPLACEMENT_RVINFO BLOB, "
          + "REPLACEMENT_HMAC BLOB, "
          + "REPLACEMENT_VOUCHER BLOB, "
          + "PRIMARY KEY (GUID), "
          + "UNIQUE (GUID)"
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "TO2_SESSIONS("
          + "SESSION_ID CHAR(36) PRIMARY KEY, "
          + "VOUCHER BLOB, "
          + "OWNER_STATE BLOB,"
          + "CIPHER_NAME VARCHAR(36), "
          + "NONCE6 BINARY(16), "
          + "NONCE7 BINARY(16), "
          + "SIGINFOA BLOB, "
          + "SERVICEINFO_COUNT BIGINT, "
          + "SERVICEINFO_POSITION BIGINT, "
          + "CREATED TIMESTAMP,"
          + "UPDATED TIMESTAMP,"
          + "PRIMARY KEY (SESSION_ID), "
          + "UNIQUE (SESSION_ID)"
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "OWNER_SERVICEINFO("
          + "SVI_ID CHAR(36) PRIMARY KEY, "
          + "MODULE_NAME CHAR(36), "
          + "MESSAGE_NAME CHAR(36),"
          + "CONTENT BLOB, "
          + "CONTENT_LENGTH BIGINT, "
          + "CONTENT_TYPE CHAR(10), "
          + "PRIMARY KEY (SVI_ID) "
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "GUID_OWNERSVI("
          + "GUID CHAR(36), "
          + "SVI_ID CHAR(36), "
          + "CREATED_AT TIMESTAMP, "
          + "PRIMARY KEY (GUID, SVI_ID),"
          + "FOREIGN KEY (GUID) references TO2_DEVICES(GUID), "
          + "FOREIGN KEY (SVI_ID) REFERENCES OWNER_SERVICEINFO(SVI_ID) "
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "GUID_DEVICEDSI("
          + "GUID CHAR(36), "
          + "DEVICE_DSI BLOB NOT NULL, "
          + "FOREIGN KEY (GUID) REFERENCES TO2_DEVICES(GUID) "
          + ");";

      stmt.executeUpdate(sql);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Imports ownership voucher.
   *
   * @param ds      A SQL datasource.
   * @param voucher The ownership voucher to import.
   */
  public void importVoucher(DataSource ds, Composite voucher) {

    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    UUID guid = ovh.getAsUuid(Const.OVH_GUID);

    String sql = ""
        + "MERGE INTO TO2_DEVICES  "
        + "KEY (GUID) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?); ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      pstmt.setBytes(2, voucher.toBytes());
      pstmt.setInt(3, 3600);
      pstmt.setInt(4, 0);
      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(5, created);
      pstmt.setTimestamp(6, null);
      pstmt.setTimestamp(7, null);
      pstmt.setTimestamp(8, null);
      pstmt.setTimestamp(9, null);
      pstmt.setString(10, guid.toString());
      pstmt.setBytes(11, ovh
          .getAsComposite(Const.OVH_RENDEZVOUS_INFO).toBytes());
      pstmt.setBytes(12, null);
      pstmt.setBytes(13, null);

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
