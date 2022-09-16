// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.util.Date;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;

public class DeviceStateApi extends RestApi {

  @Override
  protected void doGet() throws Exception {
    String path = getLastSegment();

    OnboardingVoucher onboardingVoucher = getSession().get(OnboardingVoucher.class, path);
    if (onboardingVoucher != null) {

      Date to2CompletedOn = onboardingVoucher.getTo2CompletedOn();

      StringBuilder builder = new StringBuilder();
      builder.append("{\"to2CompletedOn\" : ");
      if (to2CompletedOn != null) {
        builder.append("\"");
        builder.append(to2CompletedOn);
        builder.append("\"");
      } else {
        builder.append("null");
      }
      builder.append(",");
      builder.append("\"to0Expiry\" : ");
      Date to0Expiry = onboardingVoucher.getTo0Expiry();
      if (to0Expiry != null) {
        builder.append("\"");
        builder.append(to0Expiry);
        builder.append("\"");
      } else {
        builder.append("null");
      }
      builder.append("}");

      getResponse().getWriter().print(builder);

    } else {
      throw new NotFoundException(path);
    }
  }
}
