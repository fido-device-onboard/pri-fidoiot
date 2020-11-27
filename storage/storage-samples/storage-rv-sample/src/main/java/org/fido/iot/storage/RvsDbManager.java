// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/**
 * The Rendezvous Database Manager.
 */
public class RvsDbManager {

  /**
   * Creates Database tables.
   *
   * @param dataSource The SQL datasource.
   */
  public void createTables(DataSource dataSource) {

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {

      String sql = "CREATE TABLE IF NOT EXISTS "
          + "TO0_SESSIONS("
          + "SESSION_ID CHAR(36), "
          + "NONCE BINARY(16), "
          + "CREATED TIMESTAMP, "
          + "PRIMARY KEY (SESSION_ID), "
          + "UNIQUE (SESSION_ID) "
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "TO1_SESSIONS("
          + "SESSION_ID CHAR(36), "
          + "GUID CHAR(36), "
          + "NONCE BINARY(16), "
          + "SIGINFOA BLOB, "
          + "CREATED TIMESTAMP, "
          + "PRIMARY KEY (SESSION_ID), "
          + "UNIQUE (SESSION_ID) "
          + ");";

      stmt.executeUpdate(sql);

      sql = "CREATE TABLE IF NOT EXISTS "
          + "RV_REDIRECTS("
          + "GUID CHAR(36), "
          + "REDIRECT_BLOB BLOB, "
          + "OWNER_KEY CHAR(2048), "
          + "DEVICE_KEY BINARY(2048), "
          + "WAIT_SECONDS_RESPONSE INT, "
          + "CREATED TIMESTAMP, "
          + "EXPIRES_AT TIMESTAMP, "
          + "PRIMARY KEY (GUID), "
          + "UNIQUE (GUID) "
          + ");";

      stmt.executeUpdate(sql);

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates AllowList & DenyList database tables.
   *
   * @param dataSource The SQL datasource.
   */
  public void createAllowListDenyListTables(DataSource dataSource) {

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {

      String sql =
          "CREATE TABLE IF NOT EXISTS "
              + "GUID_DENYLIST("
              + "GUID CHAR(36), "
              + "PRIMARY KEY (GUID), "
              + "UNIQUE (GUID) "
              + ");";

      stmt.executeUpdate(sql);

      sql =
          "CREATE TABLE IF NOT EXISTS "
              + "OV_KEYS_ALLOWLIST("
              + "PUBLIC_KEY_HASH CHAR(64), "
              + "PRIMARY KEY (PUBLIC_KEY_HASH), "
              + "UNIQUE (PUBLIC_KEY_HASH) "
              + ");";

      stmt.executeUpdate(sql);

      sql =
          "CREATE TABLE IF NOT EXISTS "
              + "OV_KEYS_DENYLIST("
              + "PUBLIC_KEY_HASH CHAR(64), "
              + "PRIMARY KEY (PUBLIC_KEY_HASH), "
              + "UNIQUE (PUBLIC_KEY_HASH) "
              + ");";

      stmt.executeUpdate(sql);

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Imports GUID_DENYLIST table data.
   *
   * @param ds A SQL datasource.
   */
  public void importGuidFromDenyList(DataSource ds) {

    // list of default GUIDs to be added in denylist
    List<String> denylist = new ArrayList<>();

    String fileName = "config.properties";
    ClassLoader classLoader = getClass().getClassLoader();

    try {
      InputStream inputStream = classLoader.getResourceAsStream(fileName);
      Properties properties = new Properties();
      properties.load(inputStream);
      denylist = Arrays.asList(properties.getProperty("guid.denylist").split(","));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String sql = "MERGE INTO GUID_DENYLIST (GUID) VALUES (?);";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      for (String s : denylist) {
        pstmt.setString(1, s);
        pstmt.executeUpdate();
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Imports OV_KEYS_ALLOWLIST table data.
   *
   * @param ds A SQL datasource.
   */
  public void importAllowListKeyHash(DataSource ds) {

    // list of default public key hash to be added in allowlist
    List<String> allowlist = new ArrayList<>();

    // Loading default values in the list
    String fileName = "config.properties";
    ClassLoader classLoader = getClass().getClassLoader();

    try {
      InputStream inputStream = classLoader.getResourceAsStream(fileName);
      Properties properties = new Properties();
      properties.load(inputStream);
      allowlist = Arrays.asList(properties.getProperty("allowlist.publickeyhash").split(","));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String sql = "MERGE INTO OV_KEYS_ALLOWLIST (PUBLIC_KEY_HASH) VALUES (?);";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      for (String s : allowlist) {
        pstmt.setString(1, s);
        pstmt.executeUpdate();
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Imports OV_KEYS_DENYLIST table data.
   *
   * @param ds A SQL datasource.
   */
  public void importDenyListKeyHash(DataSource ds) {

    // list of default public key hash to be added in denylist
    List<String> denylist = new ArrayList<>();

    // Loading default values in the list
    String fileName = "config.properties";
    ClassLoader classLoader = getClass().getClassLoader();

    try {
      InputStream inputStream = classLoader.getResourceAsStream(fileName);
      Properties properties = new Properties();
      properties.load(inputStream);
      denylist = Arrays.asList(properties.getProperty("denylist.publickeyhash").split(","));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String sql = "MERGE INTO OV_KEYS_DENYLIST (PUBLIC_KEY_HASH) VALUES (?);";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      for (String s : denylist) {
        pstmt.setString(1, s);
        pstmt.executeUpdate();
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
