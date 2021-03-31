// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.serviceinfo.ModuleManager;

public class OwnerSystemResourceServlet extends HttpServlet {

  public static final String BYTES_TAG = "bytes";
  public static final String ID_TAG = "id";
  public static final String RID_TAG = "rid";
  public static final String GUID_TAG = "guid";
  public static final String DEVICE_TAG = "device";
  public static final String OS_TAG = "os";
  public static final String VERSION_TAG = "version";
  public static final String ARCH_TAG = "arch";
  public static final String FILENAME_TAG = "filename";
  public static final String PRIORITY_TAG = "priority";
  public static final String HASH_TAG = "hash";
  public static final String MODULE_TAG = "module";
  public static final String VAR_TAG = "var";
  public static final String WHERE_AND = " AND ";


  private byte[] getQueryBytes(String queryArg) {
    StringBuilder hex = new StringBuilder();
    for (char c : queryArg.toCharArray()) {
      if (Character.isLetterOrDigit(c)) {
        hex.append(c);
      }
    }

    return Composite.decodeHex(hex.toString());
  }

  private String getWhereClause(HttpServletRequest req) {
    String id = req.getParameter(ID_TAG);
    String rid = req.getParameter(RID_TAG);
    String guid = req.getParameter(GUID_TAG);
    String device = req.getParameter(DEVICE_TAG);
    String os = req.getParameter(OS_TAG);
    String version = req.getParameter(VERSION_TAG);
    String arch = req.getParameter(ARCH_TAG);
    String fileName = req.getParameter(FILENAME_TAG);
    String priority = req.getParameter(PRIORITY_TAG);
    String hash = req.getParameter(HASH_TAG);
    String module = req.getParameter(MODULE_TAG);
    String varName = req.getParameter(VAR_TAG);
    String contentType = null;

    if (module != null && varName != null) {
      contentType = module + ModuleManager.MODULE_DELIMITER + varName;
    }

    StringBuilder whereString = new StringBuilder("WHERE ");

    if (id != null) {
      whereString.append("RESOURCE_ID = ");
      whereString.append(id.toString());
    } else {
      String and = "";
      if (rid != null) {
        whereString.append(and);
        whereString.append("RESOURCE_TAG = ");
        whereString.append(rid.toString());
        and = WHERE_AND;
      }

      if (fileName != null) {
        whereString.append(and);
        whereString.append("FILE_NAME_TAG = ");
        whereString.append("'");
        whereString.append(fileName);
        whereString.append("'");
        and = WHERE_AND;
      }

      if (priority != null) {
        whereString.append(and);
        whereString.append("priority = ");
        whereString.append(priority.toString());
        and = WHERE_AND;
      }

      if (guid != null) {
        whereString.append(and);
        whereString.append("GUID_TAG = ");
        whereString.append("'");
        whereString.append(guid);
        whereString.append("'");
        and = WHERE_AND;
      }

      if (device != null) {
        whereString.append(and);
        whereString.append("DEVICE_TYPE_TAG = ");
        whereString.append("'");
        whereString.append(device);
        whereString.append("'");
        and = WHERE_AND;
      }

      if (os != null) {
        whereString.append(and);
        whereString.append("OS_NAME_TAG = ");
        whereString.append("'");
        whereString.append(os);
        whereString.append("'");
        and = WHERE_AND;
      }

      if (version != null) {
        whereString.append(and);
        whereString.append("OS_VERSION_TAG = ");
        whereString.append("'");
        whereString.append(version);
        whereString.append("'");
        and = WHERE_AND;
      }

      if (arch != null) {
        whereString.append(and);
        whereString.append("ARCHITECTURE_TAG = ");
        whereString.append("'");
        whereString.append(arch);
        whereString.append("'");
        and = WHERE_AND;
      }

      if (hash != null) {
        whereString.append(and);
        whereString.append("HASH_TAG = ");
        whereString.append("'");
        whereString.append(hash);
        whereString.append("'");
        and = WHERE_AND;
      }

      if (contentType != null) {
        whereString.append(and);
        whereString.append("CONTENT_TYPE_TAG = ");
        whereString.append("'");
        whereString.append(contentType);
        whereString.append("'");
      }
    }
    return whereString.toString();
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String sql = "SELECT "
        + "RESOURCE_ID, "
        + "CONTENT_TYPE_TAG, "
        + "RESOURCE_TAG, "
        + "PRIORITY, "
        + "FILE_NAME_TAG, "
        + "GUID_TAG, "
        + "DEVICE_TYPE_TAG, "
        + "OS_NAME_TAG, "
        + "OS_VERSION_TAG, "
        + "ARCHITECTURE_TAG, "
        + "HASH_TAG, "
        + "UPDATED, "
        + "LENGTH(CONTENT) "
        + "FROM SYSTEM_MODULE_RESOURCE " + getWhereClause(req);

    try (Connection conn = ((DataSource) getServletContext().getAttribute("datasource"))
        .getConnection();
        Statement stmt = conn.createStatement()) {

      try (ResultSet rs = stmt.executeQuery(sql)) {
        resp.setContentType("text/plain");
        try (PrintWriter writer = new PrintWriter(resp.getOutputStream())) {
          int count = 0;
          while (rs.next()) {
            count++;
            //non null values
            writer.println("id = " + rs.getString(1));
            writer.println("module-variable = " + rs.getString(2));

            //nullable values
            long resTag = rs.getLong(3);
            if (!rs.wasNull()) {
              writer.println("rid = " + Long.toString(resTag));
            }
            String value = rs.getString(5);
            if (!rs.wasNull()) {
              writer.println("filename = " + value);
            }
            value = rs.getString(6);
            if (!rs.wasNull()) {
              writer.println("guid = " + value);
            }
            value = rs.getString(7);
            if (!rs.wasNull()) {
              writer.println("device = " + value);
            }
            value = rs.getString(8);
            if (!rs.wasNull()) {
              writer.println("os = " + value);
            }
            value = rs.getString(9);
            if (!rs.wasNull()) {
              writer.println("version = " + value);
            }
            value = rs.getString(10);
            if (!rs.wasNull()) {
              writer.println("arch = " + value);
            }
            value = rs.getString(11);
            if (!rs.wasNull()) {
              writer.println("hash = " + value);
            }

            writer.println("priority = " + rs.getInt(4));

            int len = rs.getInt(13);
            if (!rs.wasNull()) {
              writer.println("content-length = " + len);
            } else {
              writer.println("content-length = none");
            }

            writer.println("updated = " + rs.getTimestamp(12)
                .toLocalDateTime().toString());

            writer.println("------------------------------------------------");
          }

          if (count == 0) {
            resp.setStatus(404); //not found
          }
        }
      }
    } catch (SQLException e) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }

  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String id = req.getParameter(ID_TAG);

    try (Connection conn = ((DataSource) getServletContext().getAttribute("datasource"))
        .getConnection();
        Statement stmt = conn.createStatement()) {

      String where = getWhereClause(req);
      String sql = "DELETE FROM SYSTEM_MODULE_RESOURCE WHERE RESOURCE_TAG = "
          + "(SELECT RESOURCE_ID FROM SYSTEM_MODULE_RESOURCE " + where + ")";

      int count1 = stmt.executeUpdate(sql);

      sql = "DELETE FROM SYSTEM_MODULE_RESOURCE " + getWhereClause(req);

      int count2 = stmt.executeUpdate(sql);
      if (count1 == 0 && count2 == 0) {
        resp.setStatus(404);
      }

    } catch (SQLException e) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }

  protected void doPut(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase("application/octet-stream") != 0) {
      resp.setStatus(415); //unsupported meida
      return;
    }

    String byteString = req.getParameter(BYTES_TAG);
    String rid = req.getParameter(RID_TAG);
    String guid = req.getParameter(GUID_TAG);
    String device = req.getParameter(DEVICE_TAG);
    String os = req.getParameter(OS_TAG);
    String version = req.getParameter(VERSION_TAG);
    String arch = req.getParameter(ARCH_TAG);
    String fileName = req.getParameter(FILENAME_TAG);
    String priority = req.getParameter(PRIORITY_TAG);
    String hash = req.getParameter(HASH_TAG);
    String module = req.getParameter(MODULE_TAG);
    String varName = req.getParameter(VAR_TAG);
    String contentType = null;

    if (module != null && varName != null) {
      contentType = module + ModuleManager.MODULE_DELIMITER + varName;
    }

    if (contentType == null) {
      resp.setStatus(400); //bad request
      return;
    }

    byte[] byteContent = null;
    if (byteString != null) {
      byteContent = getQueryBytes(byteString);
    }

    String sql = ""
        + "INSERT INTO SYSTEM_MODULE_RESOURCE ( "
        + "CONTENT,"
        + "CONTENT_TYPE_TAG, "
        + "RESOURCE_TAG, "
        + "PRIORITY, "
        + "FILE_NAME_TAG, "
        + "GUID_TAG, "
        + "DEVICE_TYPE_TAG, "
        + "OS_NAME_TAG, "
        + "OS_VERSION_TAG, "
        + "ARCHITECTURE_TAG, "
        + "HASH_TAG "
        + ") VALUES ("
        + "?,?,?,?,?,?,?,?,?,?,?)";

    try (Connection conn = ((DataSource) getServletContext().getAttribute("datasource"))
        .getConnection();
        InputStream input = req.getInputStream();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      if (rid != null) {
        pstmt.setNull(1, Types.BLOB);
        pstmt.setLong(3, Long.parseLong(rid));
      } else {
        if (byteContent == null) {
          pstmt.setBinaryStream(1, input);
        } else {
          pstmt.setBytes(1, byteContent);
        }
        pstmt.setNull(3, Types.BIGINT);
      }

      if (contentType != null) {
        pstmt.setString(2, contentType);
      } else {
        pstmt.setNull(2, Types.VARCHAR);
      }

      if (priority != null) {
        pstmt.setInt(4, Integer.parseInt(priority));
      } else {
        pstmt.setInt(4, 0);
      }

      if (fileName != null) {
        pstmt.setString(5, fileName);
      } else {
        pstmt.setNull(5, Types.VARCHAR);
      }

      if (guid != null) {
        pstmt.setString(6, guid);
      } else {
        pstmt.setNull(6, Types.VARCHAR);
      }

      if (device != null) {
        pstmt.setString(7, device);
      } else {
        pstmt.setNull(7, Types.VARCHAR);
      }

      if (os != null) {
        pstmt.setString(8, os);
      } else {
        pstmt.setNull(8, Types.VARCHAR);
      }

      if (version != null) {
        pstmt.setString(9, version);
      } else {
        pstmt.setNull(9, Types.VARCHAR);
      }

      if (arch != null) {
        pstmt.setString(10, arch);
      } else {
        pstmt.setNull(10, Types.VARCHAR);
      }
      if (hash != null) {
        pstmt.setString(11, hash);
      } else {
        pstmt.setNull(11, Types.VARCHAR);
      }

      pstmt.executeUpdate();
    } catch (SQLException e) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String byteString = req.getParameter(BYTES_TAG);

    byte[] byteContent = null;
    if (byteString != null) {
      byteContent = getQueryBytes(byteString);
    }

    String sql = ""
        + "UPDATE SYSTEM_MODULE_RESOURCE SET CONTENT = ? , UPDATED = ? "
        + getWhereClause(req);

    try (Connection conn = ((DataSource) getServletContext().getAttribute("datasource"))
        .getConnection();
        InputStream input = req.getInputStream();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      if (byteContent == null) {
        pstmt.setBinaryStream(1, input);
      } else {
        pstmt.setBytes(1, byteContent);
      }

      Timestamp updated = new Timestamp(Calendar.getInstance().getTimeInMillis());
      pstmt.setTimestamp(2,updated);

      int count = pstmt.executeUpdate();

      if (count == 0) {
        resp.setStatus(404);
      }

    } catch (SQLException e) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }

  }
}
