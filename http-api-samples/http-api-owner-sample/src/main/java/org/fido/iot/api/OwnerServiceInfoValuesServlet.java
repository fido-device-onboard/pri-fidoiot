// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.protocol.Const;
import org.fido.iot.storage.OwnerDbManager;

public class OwnerServiceInfoValuesServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase(Const.HTTP_APPLICATION_CBOR) != 0
        && req.getContentType().compareToIgnoreCase("application/octet-stream") != 0) {
      resp.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      String serviceInfoId = req.getParameter("id");
      byte[] serviceInfo = req.getInputStream().readAllBytes();
      new OwnerDbManager().addServiceInfo(ds, serviceInfoId, serviceInfo);
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }

  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String id = req.getParameter("id");
    if (id == null) {
      resp.setStatus(400);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      OwnerDbManager ownerDbManager = new OwnerDbManager();
      ownerDbManager.removeServiceInfo(ds, id);
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }
}
