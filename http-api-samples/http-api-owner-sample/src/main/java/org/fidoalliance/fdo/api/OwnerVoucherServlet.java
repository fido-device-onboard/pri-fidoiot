// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;

import org.fidoalliance.fdo.storage.OwnerDbManager;

/**
 * Manages Ownership vouchers for a To2 Server.
 */
public class OwnerVoucherServlet extends HttpServlet {

  private static final LoggerService logger = new LoggerService(OwnerVoucherServlet.class);

  protected void getVouchers(DataSource ds, OutputStream out) {
    String sql = "SELECT VOUCHER FROM TO2_DEVICES;";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Composite voucher = Composite.fromObject(rs.getBinaryStream(1));
          Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
          UUID guid = ovh.getAsUuid(Const.OVH_GUID);
          String line = guid.toString() + "\n";
          out.write(line.getBytes(StandardCharsets.UTF_8));
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets a device voucher.
   *
   * @param ds   A Datasource.
   * @param guid The device guid.
   * @return The voucher.
   */
  public byte[] getVoucher(DataSource ds, UUID guid) {

    byte[] result = Const.EMPTY_BYTE;

    String sql = "SELECT VOUCHER FROM TO2_DEVICES WHERE GUID = ?;";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Composite voucher = Composite.fromObject(rs.getBinaryStream(1));
          result = voucher.toBytes();
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return result;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase(Const.HTTP_APPLICATION_CBOR) != 0) {
      logger.warn("Request failed because of invalid content type.");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");

      Composite voucher = Composite.fromObject(req.getInputStream());
      Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
      UUID guid = ovh.getAsUuid(Const.OVH_GUID);

      new OwnerDbManager().importVoucher(ds, voucher);

      resp.setContentType("text/plain; charset=UTF-8");
      byte[] guidBytes = guid.toString().getBytes(StandardCharsets.UTF_8);
      logger.info("Imported voucher for GUID: " + guid.toString());
      resp.setContentLength(guidBytes.length);
      resp.getOutputStream().write(guidBytes);
    } catch (Exception exp) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String id = req.getParameter("id");
    if (id == null) {
      logger.warn("Request failed because of invalid input.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");

      UUID guid = UUID.fromString(id);
      new OwnerDbManager().removeVoucher(ds, guid);
      logger.info("Removed voucher with GUID: " + guid);
    } catch (Exception exp) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");

      String id = req.getParameter("id");
      if (id != null) {

        byte[] result = getVoucher(ds, UUID.fromString(id));
        if (result.length > 0) {
          resp.setContentType(Const.HTTP_APPLICATION_CBOR);
          resp.setContentLength(result.length);
          resp.getOutputStream().write(result);
        } else {
          logger.warn("Request failed because of invalid credentials.");
          resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
      } else {
        resp.setContentType("text/plain; charset=UTF-8");
        getVouchers(ds, resp.getOutputStream());
      }
    } catch (Exception exp) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
