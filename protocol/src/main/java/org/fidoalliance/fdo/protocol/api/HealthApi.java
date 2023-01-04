// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;

import java.net.URL;
import java.util.Date;

import org.fidoalliance.fdo.protocol.EpidService;
import org.fidoalliance.fdo.protocol.LoggerService;



/**
 * HealthApi returns the health status of service.
 *
 * <p>Accepted URL pattern: GET /health
 *
 * <p>RestApi Class provides a wrapper over the HttpServletRequest methods.
 */

public class HealthApi extends RestApi {

  public String databaseHealthStatus() {
    return getTransaction().getStatus().toString();
  }

  @Override
  public void doGet() throws Exception {

    LoggerService logger = new LoggerService(HealthApi.class);

    try {

      final String serviceStatus = "OK";
      String systemTime = new Date().toString();
      logger.info("Health check invoked at " + systemTime);

      final String dbHealth = databaseHealthStatus();
      String responseBody = "{\"version\" : \"%s\", \"databaseConnection\" : \"%s\", \"status\" : \"%s\"}";

      // Collects property from the service.yml file.
      String appVersion = System.getProperty("application.version");

      // Appends responseBody to the outputStream of HttpResponse Object.
      getResponse().getWriter().write(String.format(responseBody, appVersion, dbHealth, serviceStatus));
      getResponse().setContentType("application/json");

    } catch (Exception e) {
      logger.error("Unable to perform health check");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }
}
