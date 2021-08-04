// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.api;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.Debug;
import org.fidoalliance.fdo.storage.OwnerDbManager;

public class OwnerSetupInfoServlet extends HttpServlet {

  private static final String SETUPINFO_GUID = "guid";
  private static final String SETUPINFO_RVINFO = "rvinfo";
  private static final String SETUPINFO_OWNER_CUSTOMER_ID = "ownerkey";
  private static final String SETUPINFO_ARRAY_DELIMETER = ",";
  private static final String SETUPINFO_VALUE_DELIMETER = ":=";
  private static final LoggerService logger = new LoggerService(OwnerSetupInfoServlet.class);

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase("application/text") != 0) {
      logger.warn("Request failed because of invalid content type.");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    String guid = req.getParameter("id");
    if (guid == null) {
      logger.warn("Request failed because of invalid input.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    UUID currentGuid = null;
    try {
      currentGuid = UUID.fromString(guid);
    } catch (IllegalArgumentException e) {
      logger.warn("Request failed because no voucher with given GUID was found.");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    try {
      String requestBody = req.getReader().lines().collect(Collectors.joining());

      // Request format: guid:=<guid>,rvinfo:=<rvinfo>,ownerkey:=<ownerkey>
      if (requestBody != null) {
        DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
        OwnerDbManager ownerDbManager = new OwnerDbManager();

        String[] setupInfos = requestBody.split(SETUPINFO_ARRAY_DELIMETER);
        if (setupInfos.length > 3) {
          logger.warn("Request failed because of invalid SetupInfo input.");
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
        UUID replacementGuid = null;
        String replacementRvInfo = null;
        int replacementOwnerKeyCustomerId = 0;
        for (String setupInfo : setupInfos) {
          String[] si = setupInfo.split(SETUPINFO_VALUE_DELIMETER);
          if (si.length == 2) {
            switch (si[0]) {
              case SETUPINFO_GUID:
                if (Debug.IS_REUSE_DISABLED) {
                  logger.warn("Reuse not enabled. GUID cannot be set manually.");
                } else {
                  replacementGuid = UUID.fromString(si[1]);
                  logger.info("Updating Replacement GUID for " + currentGuid.toString());
                  ownerDbManager.updateDeviceReplacementGuid(ds, currentGuid, replacementGuid);
                }
                break;
              case SETUPINFO_RVINFO:
                replacementRvInfo = si[1];
                logger.info("Updating Replacement Rendezvous Info for " + currentGuid.toString());
                ownerDbManager.updateDeviceReplacementRvinfo(ds, currentGuid, replacementRvInfo);;
                break;
              case SETUPINFO_OWNER_CUSTOMER_ID:
                replacementOwnerKeyCustomerId = Integer.parseInt(si[1]);
                logger.info(
                    "Updating customer id for replacement owner key for " + currentGuid.toString());
                ownerDbManager.updateReplacementKeyCustomerId(
                    ds, currentGuid, replacementOwnerKeyCustomerId);
                break;
              default:
                break;
            }
          } else {
            logger.warn("Invalid setupinfo request has been provided.");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
          }
        }
      }
    } catch (Exception exp) {
      logger.warn("Error occurred while updating setupinfo. " + exp.getMessage());
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
