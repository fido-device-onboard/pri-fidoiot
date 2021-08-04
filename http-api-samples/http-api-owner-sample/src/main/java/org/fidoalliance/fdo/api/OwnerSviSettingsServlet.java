// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.api;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.storage.OwnerDbManager;

public class OwnerSviSettingsServlet extends HttpServlet {

  private static final String SETTINGS_DEVICE_MTU = "devicemtu";
  private static final String SETTINGS_OWNER_THRESHOLD = "ownerthreshold";
  private static final String SETTINGS_WGET_MODE_CONTENT_VERIFICATION =
      "wgetModContentVerification";
  private static final String SETUPINFO_ARRAY_DELIMETER = ",";
  private static final String SETUPINFO_VALUE_DELIMETER = ":=";
  private static final LoggerService logger = new LoggerService(OwnerSviSettingsServlet.class);

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase("application/text") != 0) {
      logger.warn("Request failed because of invalid content type.");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    try {
      String requestBody = req.getReader().lines().collect(Collectors.joining());

      /* Request format:
      devicemtu:=<mtuvalue>
       */
      if (requestBody != null) {
        DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
        OwnerDbManager ownerDbManager = new OwnerDbManager();

        String[] to2Settings = requestBody.split(SETUPINFO_ARRAY_DELIMETER);
        if (to2Settings.length > 1) {
          logger.warn("Invalid to2Settings request has been provided.");
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
        int deviceMtu = 0;
        int ownerThresholdMtu = 0;
        for (String setupInfo : to2Settings) {
          String[] setting = setupInfo.split(SETUPINFO_VALUE_DELIMETER);
          if (setting.length == 2) {
            switch (setting[0]) {
              case SETTINGS_DEVICE_MTU:
                deviceMtu = Integer.parseInt(setting[1]);
                logger.info("Updating Device MTU for TO2 Settings");
                ownerDbManager.updateMtu(ds, "DEVICE_SERVICE_INFO_MTU_SIZE", deviceMtu);
                break;
              default:
                break;
            }
          } else {
            logger.warn("Invalid settings request has been provided.");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
          }
        }
      }
    } catch (Exception exp) {
      logger.warn("Error occurred while updating TO2 settings " + exp.getMessage());
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
