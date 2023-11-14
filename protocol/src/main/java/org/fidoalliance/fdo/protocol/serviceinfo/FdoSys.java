// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serviceinfo;

/**
 * Constants common to the fdo_sys management agent and service.
 */
public final class FdoSys {

  public static final String NAME = "fdo_sys";
  public static final String ACTIVE = NAME + ":active";
  public static final String EXEC = NAME + ":exec";
  public static final String EXEC_CB = NAME + ":exec_cb";
  public static final String STATUS_CB = NAME + ":status_cb";
  public static final String FETCH = NAME + ":fetch";
  public static final String DATA = NAME + ":data";
  public static final String EOT = NAME + ":eot";
  public static final String FILEDESC = NAME + ":filedesc";
  public static final String FETCHFILE = NAME + ":fetchfile";
  public static final String WRITE = NAME + ":write";
  public static final String KEEP_ALIVE = NAME + ":keepalive";
  public static final String RET_CODE = NAME + ":retcode";
}