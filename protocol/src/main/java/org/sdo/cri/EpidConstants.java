// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0


package org.sdo.cri;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Constants class for EPID extension related constants.
 */
class EpidConstants {

  public static final int EPID1X_GID_SIZE = 4;
  public static final int EPID2X_GID_SIZE = 16;

  public static final URL onlineEpidUrlDefault;
  public static final URL sandboxEpidUrlDefault;

  static {
    try {
      onlineEpidUrlDefault = new URL("https://verify.epid.trustedservices.intel.com");
      sandboxEpidUrlDefault = new URL("https://verify.epid-sbx.trustedservices.intel.com");

    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
