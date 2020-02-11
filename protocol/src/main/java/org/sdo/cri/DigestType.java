// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.NoSuchElementException;

/**
 * SDO digest-hash type.
 *
 * @see MacType
 * @see "SDO Protocol Specification, 1.12k, 3.2.1: Hash Types and HMAC Types"
 */
enum DigestType {
  NONE(0, ""),
  SHA1(3, "SHA-1"),
  SHA256(8, "SHA-256"),
  SHA512(10, "SHA-512"),
  SHA384(14, "SHA-384");

  private final int code;
  private final String jceAlgo;

  DigestType(int code, String jceAlgo) {
    this.code = code;
    this.jceAlgo = jceAlgo;
  }

  public String toJceAlgorithm() {
    return jceAlgo;
  }

  /**
   * Convert a Number into a DigestType.
   */
  public static DigestType fromNumber(final Number n) {
    int i = n.intValue();
    for (DigestType t : DigestType.values()) {
      if (t.toInteger() == i) {
        return t;
      }
    }

    throw new NoSuchElementException(n.toString());
  }

  public int toInteger() {
    return code;
  }
}
