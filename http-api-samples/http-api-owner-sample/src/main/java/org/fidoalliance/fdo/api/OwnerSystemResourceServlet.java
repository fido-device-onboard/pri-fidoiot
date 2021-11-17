// Copyright 2021 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.api;

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

import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.serviceinfo.ModuleManager;

public class OwnerSystemResourceServlet extends HttpServlet {

  public static final String BYTES_TAG = "bytes";
  public static final String ID_TAG = "id";
  public static final String CRID_TAG = "crid";
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

  private static final LoggerService logger = new LoggerService(OwnerSystemResourceServlet.class);

  private byte[] getQueryBytes(String queryArg) {
    StringBuilder hex = new StringBuilder();
    for (char c : queryArg.toCharArray()) {
      if (Character.isLetterOrDigit(c)) {
        hex.append(c);
      }
    }

    return Composite.decodeHex(hex.toString());
  }

  private void applyValues(HttpServletRequest req, PreparedStatement stmt) throws SQLException {
    // the order of columns here must match the ones in getWhere()
    int columnId = 1;
    String id = req.getParameter(ID_TAG);
    if (id != null) {
      stmt.setLong(columnId++, Long.parseLong(id));
    }
    String crid = req.getParameter(CRID_TAG);
    if (crid != null) {
      stmt.setLong(columnId++, Long.parseLong(crid));
    }
    String fileName = req.getParameter(FILENAME_TAG);
    if (fileName != null) {
      stmt.setString(columnId++, fileName);
    }
    String priority = req.getParameter(PRIORITY_TAG);
    if (priority != null) {
      stmt.setInt(columnId++, Integer.parseInt(priority));
    }
    String guid = req.getParameter(GUID_TAG);
    if (guid != null) {
      stmt.setString(columnId++, guid);
    }
    String device = req.getParameter(DEVICE_TAG);
    if (device != null) {
      stmt.setString(columnId++, device);
    }
    String os = req.getParameter(OS_TAG);
    if (os != null) {
      stmt.setString(columnId++, os);
    }
    String version = req.getParameter(VERSION_TAG);
    if (version != null) {
      stmt.setString(columnId++, version);
    }
    String arch = req.getParameter(ARCH_TAG);
    if (arch != null) {
      stmt.setString(columnId++, arch);
    }
    String hash = req.getParameter(HASH_TAG);
    if (hash != null) {
      stmt.setString(columnId++, hash);
    }

    String module = req.getParameter(MODULE_TAG);
    String varName = req.getParameter(VAR_TAG);
    String contentType = null;

    if (module != null && varName != null) {
      contentType = module + ModuleManager.MODULE_DELIMITER + varName;
    }
    if (contentType != null) {
      stmt.setString(columnId++, contentType);
    }

  }

  private String getWhereClause(HttpServletRequest req) {
    StringBuilder whereString = new StringBuilder(" WHERE 1 = 1 ");

    String id = req.getParameter(ID_TAG);
    if (id != null) {
      whereString.append(WHERE_AND);
      whereString.append("RESOURCE_ID = ?");
    }
    String crid = req.getParameter(CRID_TAG);
    if (crid != null) {
      whereString.append(WHERE_AND);
      whereString.append("CONTENT_RESOURCE_TAG = ?");
    }
    String fileName = req.getParameter(FILENAME_TAG);
    if (fileName != null) {
      whereString.append(WHERE_AND);
      whereString.append("FILE_NAME_TAG = ?");
    }
    String priority = req.getParameter(PRIORITY_TAG);
    if (priority != null) {
      whereString.append(WHERE_AND);
      whereString.append("PRIORITY = ?");
    }
    String guid = req.getParameter(GUID_TAG);
    if (guid != null) {
      whereString.append(WHERE_AND);
      whereString.append("GUID_TAG = ?");
    }
    String device = req.getParameter(DEVICE_TAG);
    if (device != null) {
      whereString.append(WHERE_AND);
      whereString.append("DEVICE_TYPE_TAG = ?");
    }
    String os = req.getParameter(OS_TAG);
    if (os != null) {
      whereString.append(WHERE_AND);
      whereString.append("OS_NAME_TAG = ?");
    }
    String version = req.getParameter(VERSION_TAG);
    if (version != null) {
      whereString.append(WHERE_AND);
      whereString.append("OS_VERSION_TAG = ?");
    }
    String arch = req.getParameter(ARCH_TAG);
    if (arch != null) {
      whereString.append(WHERE_AND);
      whereString.append("ARCHITECTURE_TAG = ?");
    }
    String hash = req.getParameter(HASH_TAG);
    if (hash != null) {
      whereString.append(WHERE_AND);
      whereString.append("HASH_TAG = ?");
    }

    String module = req.getParameter(MODULE_TAG);
    String varName = req.getParameter(VAR_TAG);
    String contentType = null;
    if (module != null && varName != null) {
      contentType = module + ModuleManager.MODULE_DELIMITER + varName;
      logger.info("Processing request for " + contentType);
    }
    if (contentType != null) {
      whereString.append(WHERE_AND);
      whereString.append("CONTENT_TYPE_TAG = ?");
    }
    return whereString.toString();
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    logger.info("GET request.");
    String sql = "SELECT "
        + "RESOURCE_ID, "
        + "CONTENT_TYPE_TAG, "
        + "CONTENT_RESOURCE_TAG, "
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
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      applyValues(req, pstmt);
      try (ResultSet rs = pstmt.executeQuery()) {
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
            logger.warn("Request failed as required resource was not found.");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND); //not found
          }
        }
      }
    } catch (Exception e) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    logger.info("DELETE request.");
    String where = getWhereClause(req);
    String sql = "DELETE FROM SYSTEM_MODULE_RESOURCE WHERE CONTENT_RESOURCE_TAG = "
        + "(SELECT RESOURCE_ID FROM SYSTEM_MODULE_RESOURCE " + where + ")";

    try (Connection conn = ((DataSource) getServletContext().getAttribute("datasource"))
        .getConnection();
        PreparedStatement pstmt1 = conn.prepareStatement(sql)) {

      applyValues(req, pstmt1);

      int count1 = pstmt1.executeUpdate();
      sql = "DELETE FROM SYSTEM_MODULE_RESOURCE " + where;
      try (PreparedStatement pstmt2 = conn.prepareStatement(sql)) {
        applyValues(req, pstmt2);
        int count2 = pstmt2.executeUpdate();
        if (count1 == 0 && count2 == 0) {
          logger.warn("Delete request failed as required resource was not found.");
          resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
      }

    } catch (Exception e) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  protected void doPut(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    logger.info("PUT request.");
    if (req.getContentType().compareToIgnoreCase("application/octet-stream") != 0) {
      logger.warn("Request failed because of invalid content type.");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE); //unsupported meida
      return;
    }

    String byteString = req.getParameter(BYTES_TAG) == "" ? null : req.getParameter(BYTES_TAG);
    String rid = req.getParameter(CRID_TAG) == "" ? null : req.getParameter(CRID_TAG);
    String guid = req.getParameter(GUID_TAG) == "" ? null : req.getParameter(GUID_TAG);
    String device = req.getParameter(DEVICE_TAG) == "" ? null : req.getParameter(DEVICE_TAG);
    String os = req.getParameter(OS_TAG) == "" ? null : req.getParameter(OS_TAG);
    String version = req.getParameter(VERSION_TAG) == "" ? null : req.getParameter(VERSION_TAG);
    String arch = req.getParameter(ARCH_TAG) == "" ? null : req.getParameter(ARCH_TAG);
    String fileName = req.getParameter(FILENAME_TAG) == "" ? null : req.getParameter(FILENAME_TAG);
    String priority = req.getParameter(PRIORITY_TAG) == "" ? null : req.getParameter(PRIORITY_TAG);
    String hash = req.getParameter(HASH_TAG) == "" ? null : req.getParameter(HASH_TAG);
    String module = req.getParameter(MODULE_TAG) == "" ? null : req.getParameter(MODULE_TAG);
    String varName = req.getParameter(VAR_TAG) == "" ? null : req.getParameter(VAR_TAG);
    String contentType = null;

    if (module != null && varName != null) {
      contentType = module + ModuleManager.MODULE_DELIMITER + varName;
    }

    if (contentType == null) {
      logger.warn("Request failed because of invalid input.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); //bad request
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
        + "CONTENT_RESOURCE_TAG, "
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
      if (contentType != null) {
        logger.info("Updated content for " + contentType);
      }
    } catch (Exception e) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

}
