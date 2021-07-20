// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import javax.sql.DataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;

/**
 * AIO Database Manager.
 */
public class AioDbManager {

  private static LoggerService logger = new LoggerService(AioDbManager.class);
  private static final int DEFAULT_WAIT_SECONDS = 315400000;

  /**
   * Creates SQL tables for AIO.
   *
   * @param ds A SQL datasource
   */
  public void createTables(DataSource ds) {
    try (Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement()) {

      String sql =
          "CREATE TABLE IF NOT EXISTS "
              + "TO2_CONFIG ("
              + "ID INT NOT NULL DEFAULT 1, "
              + "INITIALIZED BOOLEAN NOT NULL DEFAULT FALSE, "
              + "RV_BLOB VARCHAR(4096) NOT NULL DEFAULT 'http://localhost:8080', "
              + "UNIQUE (ID)"
              + ");";

      stmt.executeUpdate(sql);

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * loads the RvBlob String.
   *
   * @param ds A SQL datasource
   * @return The string encoded redirect blob.
   */
  public String loadRvBlob(DataSource ds) {
    String result = "";
    String sql = "SELECT RV_BLOB FROM TO2_CONFIG WHERE ID = 1;";

    try (Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement()) {

      try (ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) {
          result = rs.getString(1);
        }

      } catch (SQLException e) {
        throw new RuntimeException(e);
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  /**
   * Stores a signed redirect blob to the database.
   *
   * @param guid        The quid of the device.
   * @param signedBlob  The signed redirect blob.
   * @param fingerPrint The fingerprint of the owner public key.
   * @param deviceX509  The device certificate.
   * @param ds          A SQL datasource.
   */
  public void storeRedirectBlob(String guid, byte[] signedBlob,
      String fingerPrint,
      byte[] deviceX509,
      DataSource ds) {

    String sql = ""
        + "MERGE INTO RV_REDIRECTS  "
        + "KEY (GUID) "
        + "VALUES (?,?,?,?,?,?,?);";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid);
      pstmt.setBytes(2, signedBlob);
      pstmt.setString(3, fingerPrint);
      pstmt.setBytes(4, deviceX509);
      pstmt.setInt(5, DEFAULT_WAIT_SECONDS);

      Timestamp created = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(6, created);
      Timestamp expiresAt = new Timestamp(
          Calendar.getInstance().getTimeInMillis() + (315400000 * 1000));
      pstmt.setTimestamp(7, expiresAt);

      pstmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Loads the initial SQL configuration script.
   *
   * @param ds   A sql datasource.
   * @param path the path to the sql configuration script.
   */
  public void loadInitScript(DataSource ds, String path) {
    String sql = "SELECT ID FROM TO2_CONFIG WHERE ID = 1;";
    try (Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement()) {
      boolean initalized = false;
      try (ResultSet rs = stmt.executeQuery(sql)) {
        initalized = rs.next();

      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
      if (false == initalized) {
        sql = "INSERT INTO TO2_CONFIG () VALUES ()";
        stmt.executeUpdate(sql);
        sql = loadSql(path);
        stmt.executeUpdate(sql);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Loads the new device SQL configuration script.
   *
   * @param ds   A sql datasource.
   * @param path A sql datasource.
   * @param guid The guid of the new device
   */
  public void loadNewDeviceScript(DataSource ds, String path, String guid) {
    try (Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement()) {
      String sql = loadSql(path);
      String fmtSql = String.format(sql, guid);
      stmt.executeUpdate(fmtSql);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Updaters to0RvBlob in T02_Config table.
   *
   * @param ds         A sql datasource.
   * @param to0RvBlob  rvBlob containing Owner address for device to be used in T02.
   */
  public void updateTo0RvBlob(DataSource ds, String to0RvBlob) {
    // If to0RvBlob is not empty.
    if (!to0RvBlob.equals("")) {
      String sql = "UPDATE TO2_CONFIG SET RV_BLOB = ?";
      try (Connection conn = ds.getConnection();
           PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, to0RvBlob);
        pstmt.executeUpdate();
      } catch (SQLException e) {
        logger.error("Unable to update to0_rv_blob in DB");
      }
    }
  }

  /**
   * Remove expired sessions.
   *
   * @param ds A SQL Datasource
   */
  public void removeSessions(DataSource ds) {
    try (Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement()) {
      String sql = "DELETE FROM TO0_SESSIONS WHERE  CREATED < NOW() - INTERVAL 60 SECOND";
      stmt.executeUpdate(sql);

      sql = "DELETE FROM TO2_SESSIONS WHERE  CREATED < NOW() - INTERVAL '60' SECOND";
      stmt.executeUpdate(sql);

      sql = "DELETE FROM TO2_SESSIONS WHERE  CREATED < NOW() - INTERVAL '60' SECOND";
      stmt.executeUpdate(sql);

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private String loadSql(String path) {
    String result = "";
    try {
      result = Files.readString(
          Path.of(path),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  /**
   * Get information about all devices in the database that completed DI.
   * This method is used in AioInfoServlet.
   */
  public String getDevicesInfo(DataSource ds) throws SQLException {

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode rootNode = mapper.createArrayNode();
    try (Connection conn = ds.getConnection();
         Statement stmt = conn.createStatement()) {
      StringBuilder builder = new StringBuilder();
      builder.append("SELECT GUID, SERIAL_NO, COMPLETED ");
      builder.append("FROM MT_DEVICES");

      try (ResultSet rs = stmt.executeQuery(builder.toString())) {
        while (rs.next()) {
          ObjectNode obj = mapper.createObjectNode();
          final String guid = rs.getString("GUID");
          final String serialNumber = rs.getString("SERIAL_NO");
          Timestamp timestamp = rs.getTimestamp("COMPLETED");
          obj.put("serial_no", serialNumber);
          obj.put("timestamp", timestamp.toString());
          obj.put("uuid", guid);
          rootNode.add(obj);
        }
      }
    }

    return rootNode.toString();
  }

  /**
   * Get information about devices those were registered within a given period of time.
   * This method is used in AioInfoServlet.
   */
  public String  getDevicesInfoWithTime(DataSource ds, int seconds) throws SQLException {

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode rootNode = mapper.createArrayNode();
    try (Connection conn = ds.getConnection();
         Statement stmt = conn.createStatement()) {
      StringBuilder builder = new StringBuilder();
      builder.append("SELECT GUID, SERIAL_NO, COMPLETED ");
      builder.append("FROM MT_DEVICES");

      try (ResultSet rs = stmt.executeQuery(builder.toString())) {
        while (rs.next()) {

          final String guid = rs.getString("GUID");
          final String serialNumber = rs.getString("SERIAL_NO");
          Timestamp timestamp = rs.getTimestamp("COMPLETED");
          Timestamp cur = new Timestamp(System.currentTimeMillis());
          long diff = cur.getTime() - timestamp.getTime();
          long diffSeconds = diff / 1000;
          if (diffSeconds < seconds) {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("serial_no", serialNumber);
            obj.put("timestamp", timestamp.toString());
            obj.put("uuid", guid);
            rootNode.add(obj);
          }
        }
      }
    }
    return rootNode.toString();
  }

}
