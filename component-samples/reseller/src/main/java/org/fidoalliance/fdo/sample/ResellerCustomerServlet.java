// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;

public class ResellerCustomerServlet extends HttpServlet {

  private static final LoggerService logger = new LoggerService(ResellerCustomerServlet.class);

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String id = req.getParameter("id");
    String name = req.getParameter("name");
    String contentType = req.getContentType();

    if (id.equals("") || name.equals("") || !id.matches("[0-9]+")) {
      logger.warn("Customer ID should be a number, received " + id);
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    //accept no content type or text/plain us-ascii pem
    if (contentType != null) {
      if (contentType.compareToIgnoreCase("text/plain; charset=us-ascii") != 0) {
        logger.warn("Received invalid content type.");
        resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        return;
      }
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");

      String keySet = new String(req.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

      List<PublicKey> key = PemLoader.loadPublicKeys(keySet);
      if (key.size() > 0) {
        new ResellerDbManager().defineKeySet(ds, keySet, name, Integer.parseInt(id));
      } else {
        logger.warn("PEM file is not properly formatted.");
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); //Invalid PEM string
        return;
      }

    } catch (Exception e) {
      logger.warn("Unknown error while parsing PEM file.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String uri = req.getRequestURI();
    String serialNo = uri.substring(uri.lastIndexOf('/') + 1);
    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
    int rowsAffected = new ResellerDbManager().deleteKeySet(ds, serialNo);
    if (rowsAffected == 0) {
      logger.warn("Customer ID not found.");
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
  }
}
