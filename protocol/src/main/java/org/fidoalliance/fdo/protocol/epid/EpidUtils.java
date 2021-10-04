// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.epid;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;

public class EpidUtils {

  private static String epidOnlineUrlString = "https://verify.epid-sbx.trustedservices.intel.com/";
  private static URI epidOnlineUrl = URI.create(epidOnlineUrlString);
  private static final LoggerService logger = new LoggerService(EpidUtils.class);

  /**
   * Returns EPID group id length.
   *
   * @param sgType signature type
   * @return group id length
   */
  public static int getEpidGroupIdLength(int sgType) {
    if (sgType == Const.SG_EPIDv10) {
      return Const.GID_LEN_EPIDv10;
    }
    if (sgType == Const.SG_EPIDv11) {
      return Const.GID_LEN_EPIDv11;
    }
    throw new RuntimeException(new NoSuchAlgorithmException());
  }

  /**
   * Set the EPID Online Verification Service URL.
   *
   * @param url String url
   */
  public static void setEpidOnlineUrl(String url) {
    if (null == url) {
      logger.error("EPID URL is empty");
      throw new IllegalArgumentException();
    }
    try {
      epidOnlineUrl = URI.create(url);
      logger.info("EPID Online URL: " + epidOnlineUrl.toString());
    } catch (IllegalArgumentException e) {
      logger.error("Invalid EPID URL");
      throw new IllegalArgumentException();
    }
  }

  /**
   * Return the EPID Online Verification Service URL.
   *
   * @return {@link URI} instance
   */
  public static URI getEpidOnlineUrl() {
    if (null == epidOnlineUrl) {
      throw new RuntimeException();
    }
    return epidOnlineUrl;
  }
}
