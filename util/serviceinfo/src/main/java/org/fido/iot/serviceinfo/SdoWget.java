// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants common to the sdosys management agent and service.
 */
public final class SdoWget {

  public static final Charset CHARSET = StandardCharsets.UTF_8;
  public static final String NAME = "sdo_wget";
  public static final String KEY_ACTIVE = "active";
  public static final String KEY_FILENAME = "filename";
  public static final String KEY_URL = "url";
  public static final String KEY_SHA = "sha-384";
}

