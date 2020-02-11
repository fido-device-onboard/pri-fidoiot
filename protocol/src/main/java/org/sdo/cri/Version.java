// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

public enum Version {

  VERSION_1_09(109),
  VERSION_1_10(110),
  VERSION_1_12(112),
  VERSION_1_13(113);

  private final int value;

  Version(int value) {
    this.value = value;
  }

  /**
   * Convert an integer-coded version to its enum equivalent.
   */
  public static Version valueOfInt(int value) {
    for (Version pv : Version.values()) {
      if (pv.intValue() == value) {
        return pv;
      }
    }

    throw new IllegalArgumentException(); // no match
  }

  public int intValue() {
    return value;
  }

  @Override
  public String toString() {
    return Integer.toString(intValue());
  }
}
