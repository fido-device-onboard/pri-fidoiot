// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.SecretKey;

/**
 * The SDO TO2 CTR cipher.
 */
class CtrCipher extends AbstractCipher {

  // From SDO protocol spec table 4-2:
  // <quote>
  // AES CTR Mode IV will be 16 bytes long in big-endian byte order, where:
  //
  // First 12 bytes of IV (nonce) are randomly generated at the beginning of a session,
  // independently by both sides.
  //
  // Last 4 bytes of IV (counter) is initialized to 0 at the beginning of the session.
  //
  // The IV value must be maintained with the current session key.
  // Maintain means that the IV will be changed by the underlying encryption mechanism and
  // must be copied back to the current session state for future encryption.
  //
  // For decryption, the IV will come in the header of the received message.
  // </quote>
  private final byte[] ivSeed;
  private long counter = 0; // 4 unsigned bytes won't fit in an int

  /**
   * Construct a new object.
   *
   * @param sek          The SDO Session Encryption Key (SEK)
   * @param secureRandom The source of our randomness
   */
  CtrCipher(SecretKey sek, SecureRandom secureRandom) {
    super(sek, secureRandom);
    ivSeed = new byte[12];
    new SecureRandom().nextBytes(ivSeed);
  }

  /**
   * {@inheritDoc}
   */
  protected void buildNextIv(byte[] dst) {
    ByteBuffer bb = ByteBuffer.wrap(dst);
    bb.put(ivSeed);
    bb.asIntBuffer().put((int) counter);
  }

  /**
   * {@inheritDoc}
   */
  protected String cipherTransformation() {
    return "AES/CTR/NoPadding";
  }

  /**
   * {@inheritDoc}
   */
  protected CipherText113a postEncipher(CipherText113a ct) {
    int blockCount = 1 + (ct.getCt().length - 1) / ct.getIv().length;
    counter += blockCount;
    return ct;
  }
}
