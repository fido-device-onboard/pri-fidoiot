// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * Constants common to the sdodev management agent and service.
 */
interface SdoDev {

  String NAME = "sdodev";
  String KEY_ACTIVE = NAME + ":active";
  String KEY_OS = NAME + ":os";
  String KEY_ARCH = NAME + ":arch";
  String KEY_BIN = NAME + ":bin";
  String KEY_VERSION = NAME + ":version";
  String KEY_DEVICE = NAME + ":device";
  String KEY_SN = NAME + ":sn";
  String KEY_PATHSEP = NAME + ":pathsep";
  String KEY_SEP = NAME + ":sep";
  String KEY_NL = NAME + ":nl";
  String KEY_TMP = NAME + ":tmp";
  String KEY_DIR = NAME + ":dir";
}
