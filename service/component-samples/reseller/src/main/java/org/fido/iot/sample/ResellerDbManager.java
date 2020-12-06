// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.UUID;
import javax.sql.DataSource;
import javax.xml.crypto.Data;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;

public class ResellerDbManager {

  /**
   * Create DI Tables.
   *
   * @param dataSource The datasource to use.
   */
  public void createTables(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {

      String sql = "CREATE TABLE IF NOT EXISTS "
          + "RT_DEVICES("
          + "SERIAL_NO VARCHAR(255), "
          + "VOUCHER BLOB, "
          + "CUSTOMER_ID INT NULL DEFAULT NULL, "
          + "CREATED TIMESTAMP, "
          + "PRIMARY KEY ( SERIAL_NO) "
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "RT_CUSTOMERS ("
          + "CUSTOMER_ID INT NOT NULL, "
          + "NAME VARCHAR(255), "
          + "KEYS VARCHAR(4096), "
          + "UNIQUE (CUSTOMER_ID)"
          + ");";

      stmt.executeUpdate(sql);

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Define a named Customer Key set.
   *
   * @param ds     A Datasource.
   * @param keySet A public key set.
   * @param name   Key set name.
   * @param id     Key set id.
   */
  public void defineKeySet(DataSource ds, String keySet, String name, int id) {
    String sql = ""
        + "MERGE INTO RT_CUSTOMERS  "
        + "KEY (CUSTOMER_ID) "
        + "VALUES (?,?,?); ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setInt(1, id);
      pstmt.setString(2, name);
      pstmt.setString(3, keySet);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Imports a voucher.
   *
   * @param ds         The datasource to use.
   * @param voucher    The ownership voucher to import.
   * @param serialNo   The device serial no.
   * @param customerId The customer id.
   */
  public void importVoucher(DataSource ds,
      Composite voucher,
      String serialNo,
      String customerId) {

    String sql = ""
        + "MERGE INTO RT_DEVICES  "
        + "KEY (SERIAL_NO) "
        + "VALUES (?,?,?,?); ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, serialNo);
      pstmt.setBytes(2, voucher.toBytes());
      if (customerId == null) {
        pstmt.setNull(3, Types.INTEGER);
      } else {
        pstmt.setInt(3, Integer.parseInt(customerId));
      }

      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(4, created);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Assign device to Customer.
   *
   * @param ds         The datasource to use.
   * @param serialNo   The device serialNo.
   * @param customerId The customerId
   */
  public void assignCustomer(DataSource ds, String serialNo, String customerId) {
    String sql = "UPDATE RT_DEVICES "
        + "SET CUSTOMER_ID = ? "
        + "WHERE SERIAL_NO = ?";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, Integer.parseInt(customerId));
      pstmt.setString(2, serialNo);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deletes a voucher.
   *
   * @param ds       A Datasource.
   * @param serialNo The device serialNo.
   */
  public void deleteVoucher(DataSource ds, String serialNo) {
    String sql = "DELETE FROM RT_DEVICES WHERE SERIAL_NO = ? ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, serialNo);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deletes a keyset.
   *
   * @param ds Datasource.
   * @param id Customer ID.
   */
  public void deleteKeySet(DataSource ds, String id) {
    String sql = "DELETE FROM RT_CUSTOMERS WHERE CUSTOMER_ID = ? ";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, Integer.parseInt(id));
      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
