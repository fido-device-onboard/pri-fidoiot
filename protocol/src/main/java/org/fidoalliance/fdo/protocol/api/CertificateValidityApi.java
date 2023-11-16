// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.entity.CertificateValidity;


/**
 * CertificateValidityApi REST endpoint enables users to update the default CertificateValidity
 * values in DB.
 *
 * <p>Accepted URL patterns:
 * - GET /api/v1/certificate/validity/
 * - POST /api/v1/certificate/validity?days=&ltvalue&gt
 *
 * <p>RestApi Class provides a wrapper over the HttpServletRequest methods.
 */
public class CertificateValidityApi extends RestApi {

  private static final LoggerService logger = new LoggerService(CertificateValidityApi.class);

  @Override
  public void doGet() throws Exception {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Query database table CERTIFICATE_VALIDITY for ID `1`.
    CertificateValidity certificateValidity =
        getSession().get(CertificateValidity.class, Long.valueOf(1));

    if (certificateValidity != null) {
      // if not DB is not empty, collect the number of days.
      int days = certificateValidity.getDays();
      // Append the days value to the OutputStream of HttpResponse.
      getResponse().getWriter().write(String.valueOf(days));
      getResponse().setContentType(getResponseContentType());
    } else {
      logger.warn("Certificate validity not found.");
      getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Override
  public void doPost() {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Collect parameter 'days' from HttpRequest.
    String days = getParamByValue("days");

    // Query database table CERTIFICATE_VALIDITY for ID `1`.
    CertificateValidity certificateValidity =
        getSession().get(CertificateValidity.class, Long.valueOf(1));
    try {
      int parsedDays = Integer.parseInt(days);
      if (certificateValidity == null) {
        certificateValidity = new CertificateValidity();
        certificateValidity.setDays(parsedDays);
        getSession().save(certificateValidity);
        logger.info("Certificate Validity value updated to " + parsedDays);
      } else {
        certificateValidity.setDays(parsedDays);
        getSession().update(certificateValidity);
      }
    } catch (NumberFormatException e) {
      logger.error("Invalid days parameter provided");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}