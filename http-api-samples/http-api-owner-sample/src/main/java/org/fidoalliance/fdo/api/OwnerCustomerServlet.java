// Copyright 2021 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.storage.OwnerDbManager;

public class OwnerCustomerServlet extends HttpServlet {

  private static final LoggerService logger = new LoggerService(OwnerCustomerServlet.class);

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String id = req.getParameter("id");
    String name = req.getParameter("name");
    String contentType = req.getContentType();

    if (null == id || null == name) {
      logger.warn("Request failed because of invalid input.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    //accept no content type or text/plain us-ascii pem
    if (contentType != null) {
      if (contentType.compareToIgnoreCase("text/plain; charset=us-ascii") != 0) {
        logger.warn("Request failed because of invalid content type.");
        resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        return;
      }
    }

    String keySet = new String(req.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
    PemLoader.loadPublicKeys(keySet);

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      OwnerDbManager ownerDbManager = new OwnerDbManager();
      ownerDbManager.addCustomer(ds, Integer.parseInt(id), name, keySet);
      logger.info("Added customer '" + name + "' with id '" + id + "'");
    } catch (Exception exp) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String customerId = req.getParameter("id");
    if (customerId == null) {
      logger.warn("Request failed because of invalid input.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    try {
      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      int rowsUpdated = new OwnerDbManager().removeCustomer(ds, customerId);
      if (rowsUpdated == 0) {
        logger.warn("Request failed because of invalid customer ID");
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
    } catch (Exception exp) {
      logger.warn("Request failed because of internal server error.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
