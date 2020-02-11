// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.NoSuchElementException;

/**
 * Public-Key encoding types.
 *
 * <p>These enumerations are the key encoding names from the Protocol Specification
 * with all non-alphanumerics replaced with underscores.
 *
 * @see "SDO Protocol Specification, 1.12k, 3.2.2: Public Key Types"
 */
enum KeyEncoding {
  /**
   * No public key present: key is PKNull.
   */
  NONE(0),
  /**
   * X.509.
   */
  X_509(1),
  /**
   * Modulus/exponent encoding of RSA2048RESTR or RSA_UR.
   */
  RSAMODEXP(3),
  /**
   * Intel EPID.
   *
   * @see <a href=https://01.org/epid-sdk>The Intel EPID SDK</a>
   */
  EPID(4);

  private final int value;

  private KeyEncoding(int value) {
    this.value = value;
  }

  /**
   * Convert a Number to a KeyEncoding.
   */
  public static KeyEncoding fromNumber(Number n) {

    int i = n.intValue();

    for (KeyEncoding e : values()) {

      if (e.toInteger() == i) {
        return e;
      }
    }

    throw new NoSuchElementException(KeyEncoding.class.getName() + ":" + i);
  }

  public int toInteger() {
    return value;
  }
}
