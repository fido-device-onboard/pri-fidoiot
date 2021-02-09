// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Const;

public class ResellerCustomerServlet extends HttpServlet {

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

    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");

    String keySet = new String(req.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

    PemLoader.loadPublicKeys(keySet);
    new ResellerDbManager().defineKeySet(ds, keySet, name, Integer.parseInt(id));
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String uri = req.getRequestURI();
    String serialNo = uri.substring(uri.lastIndexOf('/') + 1);
    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
    new ResellerDbManager().deleteKeySet(ds, serialNo);
  }
}
