// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.fido.iot.protocol.Const;
import org.fido.iot.storage.OwnerDbManager;

public class OwnerSetupInfoServlet extends HttpServlet {

  private static final String SETUPINFO_GUID = "guid";
  private static final String SETUPINFO_RVINFO = "rvinfo";
  private static final String SETUPINFO_ARRAY_DELIMETER = ",";
  private static final String SETUPINFO_VALUE_DELIMETER = ":=";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getContentType().compareToIgnoreCase("application/text") != 0) {
      resp.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    String guid = req.getParameter("id");
    if (guid == null) {
      getServletContext().log("No current GUID has been provided to update.");
      resp.setStatus(400);
      return;
    }
    UUID currentGuid = null;
    try {
      currentGuid = UUID.fromString(guid);
    } catch (IllegalArgumentException e) {
      getServletContext().log("Invalid current GUID has been provided to update.");
      resp.setStatus(400);
      return;
    }

    try {
      String requestBody = req.getReader().lines().collect(Collectors.joining());

      // Request format: guid==<guid>,rvinfo==<rvinfo>
      if (requestBody != null) {
        DataSource ds = (DataSource) getServletContext().getAttribute("datasource");
        OwnerDbManager ownerDbManager = new OwnerDbManager();

        String[] setupInfos = requestBody.split(SETUPINFO_ARRAY_DELIMETER);
        if (setupInfos.length > 2) {
          getServletContext().log("Invalid setupinfo request has been provided.");
          resp.setStatus(400);
          return;
        }
        UUID replacementGuid = null;
        String replacementRvInfo = null;
        for (String setupInfo : setupInfos) {
          String[] si = setupInfo.split(SETUPINFO_VALUE_DELIMETER);
          if (si.length == 2) {
            switch (si[0]) {
              case SETUPINFO_GUID:
                replacementGuid = UUID.fromString(si[1]);
                getServletContext().log("Updating Replacement GUID for " + currentGuid.toString());
                ownerDbManager.updateDeviceReplacementGuid(ds, currentGuid, replacementGuid);
                break;
              case SETUPINFO_RVINFO:
                replacementRvInfo = si[1];
                getServletContext()
                    .log("Updating Replacement Rendezvous Info for " + currentGuid.toString());
                ownerDbManager.updateDeviceReplacementRvinfo(ds, currentGuid, replacementRvInfo);;
                break;
              default:
                break;
            }
          } else {
            getServletContext().log("Invalid setupinfo request has been provided.");
            resp.setStatus(400);
            return;
          }
        }
      }
    } catch (Exception exp) {
      getServletContext().log("Error occurred while updating setupinfo. " + exp.getMessage());
      resp.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
    }
  }
}
