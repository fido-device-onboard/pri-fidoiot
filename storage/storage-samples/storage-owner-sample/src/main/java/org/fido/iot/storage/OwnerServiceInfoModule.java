// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.serviceinfo.ServiceInfoEntry;
import org.fido.iot.serviceinfo.ServiceInfoModule;
import org.fido.iot.serviceinfo.ServiceInfoSequence;

public class OwnerServiceInfoModule implements ServiceInfoModule {

  private DataSource dataSource;

  public OwnerServiceInfoModule(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  // Return List of owner serviceinfo entries parsed by moduleName:messageName and value
  // General idea is that the serviceinfo is fetched in their insertion order. Service info
  // must be inserted into the storage in the exact order (using ascending timestamp)
  // that they are supposed to be transferred.
  @Override
  public List<ServiceInfoEntry> getServiceInfo(UUID uuid) {
    List<ServiceInfoEntry> serviceInfoEntries = new LinkedList<ServiceInfoEntry>();

    List<String> serviceInfoIds = new LinkedList<String>();
    String sql = "SELECT SVI_ID FROM GUID_OWNERSVI WHERE GUID = ? ORDER BY CREATED_AT ASC;";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, uuid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          serviceInfoIds.add(rs.getString(1));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    Iterator<String> itr = serviceInfoIds.listIterator();
    while (itr.hasNext()) {
      String serviceInfoId = itr.next();
      sql = "SELECT MODULE_NAME, MESSAGE_NAME FROM OWNER_SERVICEINFO WHERE SVI_ID = ?";
      try (Connection conn = dataSource.getConnection();
          PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, serviceInfoId);
        try (ResultSet rs = pstmt.executeQuery()) {
          while (rs.next()) {
            String key = String.format("%1$s:%2$s", rs.getString(1), rs.getString(2));
            ServiceInfoSequence valueSequence =
                new OwnerServiceInfoSequence(serviceInfoId, dataSource);
            valueSequence.initSequence();
            serviceInfoEntries.add(new ServiceInfoEntry(key, valueSequence));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    return serviceInfoEntries;
  }

  @Override
  public void putServiceInfo(UUID uuid, ServiceInfoEntry entry) {
    String sql = "MERGE INTO GUID_DEVICEDSI KEY (GUID) VALUES(?,?)";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, uuid.toString());
      pstmt.setBytes(2, (byte[]) entry.getValue().getContent());

      pstmt.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
