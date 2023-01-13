// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;

/**
 * Maintains To2Blob for owners.
 */
public class To2Blob extends RestApi {

  @Override
  protected void doGet() throws Exception {
    getTransaction();
    OnboardingConfig onboardConfig =
        getSession().get(OnboardingConfig.class, Long.valueOf(1));

    if (onboardConfig != null) {

      String body = onboardConfig.getRvBlob().getSubString(1,
          Long.valueOf(onboardConfig.getRvBlob().length()).intValue());

      getResponse().getWriter().print(body);
    }

  }

  @Override
  public void doPost() throws Exception {
    String body = getStringBody();

    try {
      final To2AddressEntries to2AddressEntries =
            Mapper.INSTANCE.readJsonValue(body, To2AddressEntries.class);
      HttpUtils.getInstructions(to2AddressEntries);
    } catch (Exception e) {
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

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
