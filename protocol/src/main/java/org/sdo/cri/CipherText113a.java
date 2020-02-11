// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.Arrays;

/**
 * The SDO 'Encrypted Message Body' cipher-text, or 'ct'.
 *
 * <p>CipherText objects are the output of {@link ProtocolCipher#encipher}
 * and the input of {@link ProtocolCipher#decipher}.
 */
class CipherText113a {

  private final byte[] ct;
  private final byte[] iv;

  CipherText113a(byte[] iv, byte[] ct) {
    this.iv = Arrays.copyOf(iv, iv.length);
    this.ct = Arrays.copyOf(ct, ct.length);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CipherText113a that = (CipherText113a) o;
    return Arrays.equals(ct, that.ct)
        && Arrays.equals(iv, that.iv);
  }

  public byte[] getCt() {
    return Arrays.copyOf(ct, ct.length);
  }

  public byte[] getIv() {
    return Arrays.copyOf(iv, iv.length);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(ct);
    result = 31 * result + Arrays.hashCode(iv);
    return result;
  }
}
