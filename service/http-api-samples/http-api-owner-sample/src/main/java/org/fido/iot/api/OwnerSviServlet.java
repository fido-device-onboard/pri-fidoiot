// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.protocol.Const;
import org.fido.iot.storage.OwnerDbManager;

public class OwnerSviServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase("application/text") != 0) {
      resp.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    UUID guid = UUID.fromString(req.getParameter("guid"));
    String sviDelimeted = req.getReader().lines().collect(Collectors.joining());
    if (guid == null || sviDelimeted == null) {
      resp.setStatus(400);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      OwnerDbManager ownerDbManager = new OwnerDbManager();
      // remove previous associations by default, since we have to upload the entire SVI list
      ownerDbManager.removeSviFromDevice(ds, guid);
      ownerDbManager.assignSviToDevice(ds, guid, sviDelimeted);
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    UUID guid = UUID.fromString(req.getParameter("guid"));
    if (guid == null) {
      resp.setStatus(400);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      new OwnerDbManager().removeSviFromDevice(ds, guid);
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }
}
