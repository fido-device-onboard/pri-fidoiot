// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.storage.DiDbManager;

public class RvInfoServlet extends HttpServlet {

  final String validUriFormat =
      "((http|https)://[a-zA-Z0-9.-]*:[0-9]{1,5}[?]?(([a-zA-Z]+=[A-z0-9.-]*)[&]?)*[\\s]?)*";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String contentType = req.getContentType();

    if (contentType != null) {
      if (contentType.compareToIgnoreCase("text/plain; charset=us-ascii") != 0) {
        resp.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
        return;
      }
    }

    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");

    String rvInfo = new String(req.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

    if (rvInfo != null && rvInfo.length() != 0 && rvInfo.matches(validUriFormat)) {
      DiDbManager dbManager = new DiDbManager();
      dbManager.addRvInfo(ds, rvInfo);
    } else {
      resp.setStatus(400);
      return;
    }
  }
}
