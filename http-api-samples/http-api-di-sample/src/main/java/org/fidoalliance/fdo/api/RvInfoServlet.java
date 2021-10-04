// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.RendezvousInfoDecoder;
import org.fidoalliance.fdo.storage.DiDbManager;

public class RvInfoServlet extends HttpServlet {

  private static final LoggerService logger = new LoggerService(RvInfoServlet.class);

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String contentType = req.getContentType();

    if (contentType != null) {
      if (contentType.compareToIgnoreCase("text/plain; charset=us-ascii") != 0) {
        logger.warn("Request failed because of unsupported content type.");
        resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        return;
      }
    }
    try {

      DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
      String rvInfo = new String(req.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

      Composite rvi = Composite.fromObject(rvInfo);
      List<String> directives = RendezvousInfoDecoder
              .getHttpDirectives(rvi,Const.RV_DEV_ONLY);
      if (directives.size() > 0 && RendezvousInfoDecoder.sanityCheck(rvi)) {
        DiDbManager dbManager = new DiDbManager();
        dbManager.addRvInfo(ds, rvInfo);
        logger.info("Updated RVInfo.");
      } else {
        //If we are unable to resolve even one directive, then we return 400 BAD_REQUEST.
        logger.warn("Request failed because of invalid RVInfo.");
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
    } catch (Exception e) {
      logger.warn("Request failed because of invalid RVInfo.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
  }
}
