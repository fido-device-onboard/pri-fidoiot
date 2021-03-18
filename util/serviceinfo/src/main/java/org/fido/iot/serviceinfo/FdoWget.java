// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants common to the sdosys management agent and service.
 */
public final class FdoWget {

  public static final Charset CHARSET = StandardCharsets.UTF_8;
  public static final String NAME = "fdo_wget";
  public static final String KEY_ACTIVE = NAME + ":active";
  public static final String KEY_FILENAME = NAME + ":filename";
  public static final String KEY_URL = NAME + ":url";
  public static final String KEY_SHA = NAME + ":sha-384";
}

