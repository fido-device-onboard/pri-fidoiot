// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import java.util.NoSuchElementException;

public enum CborTags {
  COSE_ENCRYPT_0(16),
  COSE_MAC_0(17),
  COSE_SIGN_1(18);

  private final int id;

  CborTags(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  public static CborTags fromNumber(Number n) {
    int i = n.intValue();

    for (CborTags e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(CborTags.class.getName() + ":" + i);
  }

  public int toInteger() {
    return id;
  }
}
