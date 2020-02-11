// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants common to the sdosys management agent and service.
 */
public interface SdoSys {

  String NAME = "sdo_sys";
  Charset CHARSET = StandardCharsets.UTF_8;
  String KEY_ACTIVE = NAME + ":active";
  String KEY_EXEC = NAME + ":exec";
  String KEY_FILEDESC = NAME + ":filedesc";
  String KEY_WRITE = NAME + ":write";
}
