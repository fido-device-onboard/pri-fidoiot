// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants common to the sdosys management agent and service.
 */
public final class SdoSys {

  public static final Charset CHARSET = StandardCharsets.UTF_8;
  public static final String NAME = "sdo_sys";
  public static final String KEY_ACTIVE = "active";
  public static final String KEY_EXEC = "exec";
  public static final String KEY_FILEDESC = "filedesc";
  public static final String KEY_WRITE = "write";

}
