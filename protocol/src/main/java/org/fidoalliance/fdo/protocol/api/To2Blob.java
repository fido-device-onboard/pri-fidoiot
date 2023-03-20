// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;

/**
 * Maintains To2Blob for owners.
 */
public class To2Blob extends RestApi {
  LoggerService logger = new LoggerService(To2Blob.class);

  @Override
  protected void doGet() throws Exception {
    getTransaction();
    OnboardingConfig onboardConfig =
        getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (onboardConfig != null) {

      String body = onboardConfig.getRvBlob().getSubString(1,
          Long.valueOf(onboardConfig.getRvBlob().length()).intValue());
      logger.info("TO2 body: " + body);

      getResponse().getWriter().print(body);
    }

  }

  @Override
  public void doPost() throws Exception {
    String body = getStringBody();
    logger.info("TO2 body: " + body);

    getTransaction();

    OnboardingConfig onboardingConfig =
        getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (onboardingConfig == null) {
      onboardingConfig = new OnboardingConfig();
      onboardingConfig.setRvBlob(getSession().getLobHelper()
          .createClob(body));
      getSession().save(onboardingConfig);

    } else {
      onboardingConfig.setRvBlob(getSession().getLobHelper()
          .createClob(body));
      getSession().update(onboardingConfig);
    }
  }
}
