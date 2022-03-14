// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;


/**
 * SviSizeApi REST endpoint enables users to set and collect MAX_SERVICEINFO_SIZE value.
 *
 * <p>Accepted URL patterns : - GET /api/v1/owner/svisize
 *                         - POST /api/v1/owner/svisize with message size value in body.
 *
 * <p>RestApi Class provides a wrapper over the HttpServletRequest methods.
 */
public class SviSizeApi extends RestApi {

  private static final LoggerService logger = new LoggerService(SviSizeApi.class);

  @Override
  public void doGet() throws Exception {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Query database table ONBOARDING_CONFIG for id `1`.
    OnboardingConfig config = getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (config != null) {
      // if config is not empty, return the MessageSize in HttpResponse body.
      if (config.getMaxServiceInfoSize() != null) {
        int messageSize = config.getMaxServiceInfoSize();
        getResponse().getWriter().write(String.valueOf(messageSize));
        getResponse().setContentType(getResponseContentType());
      } else {
        getResponse().getWriter().write(String.valueOf(0));
        getResponse().setContentType(getResponseContentType());
      }
    } else {
      logger.warn("Empty Onboarding Config Table.");
      getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

  }

  @Override
  public void doPost() throws Exception {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Performing input checks on the SVI message size.
    int messageSize = 0;
    try {
      messageSize = Integer.valueOf(getStringBody());
    } catch (NumberFormatException e) {
      logger.error("Invalid SVI Threshold size");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (messageSize < 1300) {
      logger.warn("Value below the threshold of 1300 bytes. Defaulting to 1300 bytes.");
      messageSize = 1300;
    } else if (messageSize > 65536) {
      logger.warn("Value above the threshold of 65536 bytes. Defaulting to 65536 bytes.");
      messageSize = 65536;
    }

    // Query database table ONBOARDING_CONFIG for id `1`.
    OnboardingConfig config = getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (config == null) {
      // If config is empty, insert new row into DB.
      config = new OnboardingConfig();
      config.setMaxMessageSize(null);
      config.setReplacementRvInfo(null);
      config.setMaxServiceInfoSize(messageSize);
      getSession().save(config);
    } else {
      // Update value of MAX_SERVICEINFO_SIZE column in ONBOARDING_CONFIG table.
      config.setMaxServiceInfoSize(messageSize);
      getSession().update(config);
    }

  }

}
