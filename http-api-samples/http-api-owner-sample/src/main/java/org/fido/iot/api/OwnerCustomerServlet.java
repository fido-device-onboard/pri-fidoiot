// Copyright 2021 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Const;
import org.fido.iot.storage.OwnerDbManager;

public class OwnerCustomerServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String id = req.getParameter("id");
    String name = req.getParameter("name");
    String contentType = req.getContentType();

    if (null == id || null == name) {
      resp.setStatus(400);
      return;
    }

    //accept no content type or text/plain us-ascii pem
    if (contentType != null) {
      if (contentType.compareToIgnoreCase("text/plain; charset=us-ascii") != 0) {
        resp.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
        return;
      }
    }

    String keySet = new String(req.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
    PemLoader.loadPublicKeys(keySet);

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      OwnerDbManager ownerDbManager = new OwnerDbManager();
      ownerDbManager.addCustomer(ds, Integer.parseInt(id), name, keySet);
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String customerId = req.getParameter("id");
    if (customerId == null) {
      resp.setStatus(400);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      new OwnerDbManager().removeCustomer(ds, customerId);
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }
}
