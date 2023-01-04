// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;

import org.fidoalliance.fdo.protocol.LoggerService;



/**
 * HealthApi returns the health status of service.
 *
 * <p>Accepted URL pattern: GET /health
 *
 * <p>RestApi Class provides a wrapper over the HttpServletRequest methods.
 */

public class HealthApi extends RestApi {

  @Override
  public void doGet() throws Exception {

    LoggerService logger = new LoggerService(HealthApi.class);

    try {

      String systemTime = new Date().toString();
      logger.info("Health check invoked at " + systemTime);
      String responseBody = "{\"version\" : \"%s\", \"status\" : \"OK\"}";

      // Collects property from the service.yml file.
      String appVersion = System.getProperty("application.version");

      // Appends responseBody to the outputStream of HttpResponse Object.
      getResponse().getWriter().write(String.format(responseBody, appVersion));
      getResponse().setContentType("application/json");

    } catch (Exception e) {
      logger.error("Unable to perform health check");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }
}
