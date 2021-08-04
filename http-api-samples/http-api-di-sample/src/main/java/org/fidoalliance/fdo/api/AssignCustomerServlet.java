// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.api;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.storage.DiDbManager;

public class AssignCustomerServlet extends HttpServlet {

  private static final LoggerService logger = new LoggerService(AssignCustomerServlet.class);

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");

    try {

      String serial = req.getParameter("serial");
      String id = req.getParameter("id");
      if (id.equals("") && serial.equals("") && !id.matches("[0-9]+")) {
        logger.warn("Request failed because invalid input.");
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

      int rowsAffected = new DiDbManager()
          .assignCustomerToVoucher(ds, Integer.parseInt(id), serial);
      if (rowsAffected == 0) {
        logger.warn("Request failed because device with given serial was not found.");
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      } else {
        logger.info("Device with serial " + serial + " assigned to customer with id " + id);
      }
    } catch (RuntimeException e) {
      logger.warn("Request failed because of invalid input.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    } catch (Exception e) {
      logger.warn("Request failed because server couldn't process the request.");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
  }
}
