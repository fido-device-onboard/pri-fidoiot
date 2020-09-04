// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
}
