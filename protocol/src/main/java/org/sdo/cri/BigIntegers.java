// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.math.BigInteger;
import java.util.Arrays;

class BigIntegers {

  /**
   * Converts a BigInteger into a byte[].
   *
   * <p>BigInteger arrays can be sign extended and shorter or longer
   * than the expected number of bytes.
   * Trim or pad these arrays as needed.
   */
  public static byte[] toByteArray(BigInteger shx, int arraySize) {

    byte[] shxBytes = shx.toByteArray();

    int delta = arraySize - shxBytes.length;

    if (delta < 0) {
      // shxBytes is too long.  Truncate it.
      return Arrays.copyOfRange(shxBytes, -delta, shxBytes.length);

    } else if (delta > 0) {
      // shxBytes is too short.  Sign-extend it.
      byte[] result = new byte[arraySize];
      byte fillByte = shx.signum() < 0 ? (byte) 0xff : (byte) 0;
      Arrays.fill(result, 0, delta, fillByte);
      System.arraycopy(shxBytes, 0, result, delta, shxBytes.length);
      return result;

    } else {
      return shxBytes;
    }
  }
}
