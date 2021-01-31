// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

public class OwnerDbTo0Util {

  /**
   * Return {@link List} of {@link UUID} whose TO0 is either not done, or has expired.
   *
   * @param dataSource A SQL datasource
   * @return
   */
  public List<UUID> fetchDevicesForTo0(DataSource dataSource) {
    String sql = "SELECT GUID FROM TO2_DEVICES WHERE WAIT_SECONDS_RESPONSE = 0 "
        + "OR TO0_COMPLETED IS NULL "
        + "OR TIMESTAMPADD(SECOND, WAIT_SECONDS_RESPONSE, TO0_COMPLETED) <  CURRENT_TIMESTAMP();";

    List<UUID> uuids = new LinkedList<UUID>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          uuids.add(UUID.fromString(rs.getString(1)));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return uuids;
  }

  /**
   * Return the stored wait seconds as sent by Rendezvous during TO0 for the device.
   * 
   * @param dataSource A SQL datasource
   * @param guid Device guid
   * @return
   */
  public long getResponseWait(DataSource dataSource, UUID guid) {
    String sql = "SELECT WAIT_SECONDS_RESPONSE FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return 0;
  }
}
