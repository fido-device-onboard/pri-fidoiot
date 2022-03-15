// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;

/**
 * MessageSizeApi REST endpoint enables users to set and collect MAX_MESSAGE_SIZE value.
 * MAX_MESSAGE_SIZE value is used in msg/61.
 *
 * <p>Accepted URL patterns : - GET /api/v1/owner/messagesize
 *                         - POST /api/v1/owner/messagesize with message size value in body.
 *
 * <p>RestApi Class provides a wrapper over the HttpServletRequest methods.
 */

public class MessageSizeApi extends RestApi {

  private static final LoggerService logger = new LoggerService(MessageSizeApi.class);

  @Override
  public void doGet() throws Exception {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    // Query database table ONBOARDING_CONFIG for id `1`.
    OnboardingConfig config = getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (config != null) {
      // if config is not empty, return the MessageSize in HttpResponse body.
      if (config.getMaxMessageSize() != null) {
        int messageSize = config.getMaxMessageSize();
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

    int messageSize = 0;
    try {
      messageSize = Integer.valueOf(getStringBody());
    } catch (NumberFormatException e) {
      logger.error("Invalid SVI Threshold size");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // Performing input checks on the message size.
    if (messageSize < 0) {
      logger.warn("Value below the threshold of 0 bytes. Defaulting to 1500 bytes.");
      messageSize = 1500;
    } else if (messageSize > 1500) {
      logger.warn("Value above the threshold of 1500 bytes. Defaulting to 1500 bytes.");
      messageSize = 1500;
    }

    // Query database table ONBOARDING_CONFIG for id `1`.
    OnboardingConfig config = getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (config == null) {
      // If config is empty, insert new row into DB.
      config = new OnboardingConfig();
      config.setMaxServiceInfoSize(null);
      config.setReplacementRvInfo(null);
      config.setMaxMessageSize(messageSize);
      getSession().save(config);
    } else {
      // Update value of MAX_MESSAGE_SIZE column in ONBOARDING_CONFIG table.
      config.setMaxMessageSize(messageSize);
      getSession().update(config);
    }

  }

}
