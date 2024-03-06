// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;


/**
 * Maintains To2Blob for owners.
 */
public class RvReplacementApi extends RestApi {
  LoggerService logger = new LoggerService(RvReplacementApi.class);

  @Override
  protected void doGet() throws Exception {
    getTransaction();
    OnboardingConfig onboardConfig =
        getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (onboardConfig.getReplacementRvInfo() != null) {

      String body = onboardConfig.getReplacementRvInfo().getSubString(1,
          Long.valueOf(onboardConfig.getReplacementRvInfo().length()).intValue());
      logger.info("RvReplacement body: " + body);

      getResponse().getWriter().print(body);
    } else {
      getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

  }

  @Override
  public void doPost() throws Exception {

    getTransaction();

    String body = getStringBody();
    logger.info("RvReplacement body: " + body);

    try {
      Mapper.INSTANCE.readJsonValue(body, RendezvousInfo.class);
    } catch (Exception e) {
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }



    OnboardingConfig onboardingConfig =
        getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (onboardingConfig == null) {
      onboardingConfig = new OnboardingConfig();
      onboardingConfig.setReplacementRvInfo(getSession().getLobHelper()
          .createClob(body));
      getSession().save(onboardingConfig);

    } else {
      onboardingConfig.setReplacementRvInfo(getSession().getLobHelper()
          .createClob(body));
      getSession().update(onboardingConfig);
    }
  }
}
