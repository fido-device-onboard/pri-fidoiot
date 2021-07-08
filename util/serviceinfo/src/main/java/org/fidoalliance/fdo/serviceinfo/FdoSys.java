// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.serviceinfo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants common to the fdo_sys management agent and service.
 */
public final class FdoSys {

  public static final String NAME = "fdo_sys";
  public static final String KEY_ACTIVE = NAME + ":active";
  public static final String KEY_EXEC = NAME + ":exec";
  public static final String KEY_FILEDESC = NAME + ":filedesc";
  public static final String KEY_WRITE = NAME + ":write";
  public static final String KEY_KEEP_ALIVE = NAME + ":keepalive";
  public static final String KEY_RET_CODE = NAME + ":retcode";
  public static final String KEY_IS_DONE = NAME + ":isdone";
  public static final String KEY_IS_MORE = NAME + ":ismore";
}

