// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.storage.DiDbManager;

public class AssignCustomerServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    DataSource ds = (DataSource) getServletContext().getAttribute("datasource");

    String guid = req.getParameter("guid");
    String id = req.getParameter("id");
    if (id != null && guid != null) {
      new DiDbManager().assignCustomerToVoucher(ds, Integer.parseInt(id), guid);
    }
  }
}
