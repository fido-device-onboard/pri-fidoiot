// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.InvalidKeyException;

/**
 * The common interface for all SDO TO2 ciphers.
 */
interface ProtocolCipher {

  /**
   * Deciphers the given {@link CipherText113a}.
   *
   * @param in the {@link CipherText113a}.
   *
   * @return the deciphered byte array.
   *
   * @throws InvalidKeyException If the cipher does not have a valid key.
   */
  byte[] decipher(CipherText113a in) throws InvalidKeyException;

  /**
   * Enciphers the given byte array.
   *
   * @param in the input bytes.
   *
   * @return the resultant {@link CipherText113a}.
   *
   * @throws InvalidKeyException If the cipher does not have a valid key.
   */
  CipherText113a encipher(byte[] in) throws InvalidKeyException;
}
