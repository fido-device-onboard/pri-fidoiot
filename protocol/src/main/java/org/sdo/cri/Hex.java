// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class Hex {
  private static final char[] DIGITS = "0123456789ABCDEF".toCharArray();

  static String toHexString(byte[] bytes) {
    StringBuilder stringBuilder = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      stringBuilder.append(DIGITS[(b >> 4) & 0x0f]);
      stringBuilder.append(DIGITS[b & 0x0f]);
    }
    return stringBuilder.toString();
  }
}
