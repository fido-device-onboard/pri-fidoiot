// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

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
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;

import org.fido.iot.storage.OwnerDbManager;

/**
 * Manages Ownership vouchers for a To2 Server.
 */
public class OwnerReplacementVoucherServlet extends HttpServlet {

  /**
   * Gets a device voucher.
   *
   * @param ds   A Datasource.
   * @param guid The device guid.
   * @return The voucher.
   */
  public byte[] getVoucher(DataSource ds, UUID guid) {

    byte[] result = Const.EMPTY_BYTE;

    String sql = "SELECT REPLACEMENT_VOUCHER FROM TO2_DEVICES WHERE "
        + " TO2_COMPLETED IS NOT NULL AND GUID = ?;";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, guid.toString());
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          InputStream is = rs.getBinaryStream(1);
          if (is != null) {
            Composite voucher = Composite.fromObject(is);
            result = voucher.toBytes();
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return result;
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
          getServletContext().log("No replacement voucher found for " + id);
          resp.setStatus(401);
        }
      }
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }
}
