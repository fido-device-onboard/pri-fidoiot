// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.SecureRandom;
import javax.crypto.SecretKey;

/**
 * The SDO TO2 CBC cipher.
 */
class CbcCipher extends AbstractCipher {

  CbcCipher(SecretKey sek, SecureRandom secureRandom) {
    super(sek, secureRandom);
  }

  /**
   * {@inheritDoc}
   */
  protected String cipherTransformation() {
    return "AES/CBC/PKCS7Padding";
  }
}
