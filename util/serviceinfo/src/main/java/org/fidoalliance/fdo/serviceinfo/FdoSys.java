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
  public static final String KEY_EXEC_CB = NAME + ":exec_cb";
  public static final String KEY_STATUS_CB = NAME + ":status_cb";
  public static final String KEY_FETCH = NAME + ":fetch";
  public static final String KEY_DATA = NAME + ":data";
  public static final String KEY_EOT = NAME + ":eot";
  public static final String KEY_FILEDESC = NAME + ":filedesc";
  public static final String KEY_WRITE = NAME + ":write";
  public static final String KEY_IS_DONE = NAME + ":isdone";
  public static final String KEY_IS_MORE = NAME + ":ismore";

}

