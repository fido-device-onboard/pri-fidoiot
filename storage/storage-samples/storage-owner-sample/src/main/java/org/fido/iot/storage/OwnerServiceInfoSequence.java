// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.fido.iot.serviceinfo.ServiceInfoSequence;

public class OwnerServiceInfoSequence extends ServiceInfoSequence {

  private DataSource dataSource;

  public static final String CBOR_TYPE = "cbor";
  public static final String PLAIN_TYPE = "plain";

  public OwnerServiceInfoSequence(String id, DataSource ds) {
    super(id);
    dataSource = ds;
  }

  @Override
  public long length() {
    String sql = "SELECT CONTENT_LENGTH FROM OWNER_SERVICEINFO WHERE SVI_ID = ?;";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, getServiceInfoId());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException();
  }

  @Override
  public Object getContent() {
    int lengthToRead = (int) (getEnd() - getStart());
    String sql = "SELECT CONTENT FROM OWNER_SERVICEINFO WHERE SVI_ID = ?;";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, getServiceInfoId());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          InputStream inputStream = rs.getBlob(1).getBinaryStream(getStart() + 1, lengthToRead);
          byte[] content = new byte[lengthToRead];
          inputStream.read(content, 0, lengthToRead);
          inputStream.close();
          return content;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException();
  }

  @Override
  public boolean canSplit() {
    String sql = "SELECT CONTENT_TYPE FROM OWNER_SERVICEINFO WHERE SVI_ID = ?;";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, getServiceInfoId());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          String value = rs.getString(1);
          if (value.equalsIgnoreCase(PLAIN_TYPE)) {
            return true;
          } else if (value.equalsIgnoreCase(CBOR_TYPE)) {
            return false;
          } else {
            return false;
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return false;
  }
}
