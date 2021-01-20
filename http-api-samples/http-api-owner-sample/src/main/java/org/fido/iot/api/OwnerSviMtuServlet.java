// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.protocol.Const;
import org.fido.iot.storage.OwnerDbManager;

public class OwnerSviMtuServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase("application/text") != 0) {
      resp.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    String id = req.getParameter("id");
    if (id == null) {
      getServletContext().log("No current ID has been provided to update.");
      resp.setStatus(400);
      return;
    }

    try {
      String requestBody = req.getReader().lines().collect(Collectors.joining());

      int mtu = Integer.parseInt(requestBody);

      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      OwnerDbManager ownerDbManager = new OwnerDbManager();
      // update the maximum MTU owner can accept
      ownerDbManager.updateDeviceMtu(ds, Integer.parseInt(id), mtu);
    } catch (Exception exp) {
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }
}
