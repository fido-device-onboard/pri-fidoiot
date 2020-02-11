// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.SecretKey;

abstract class SessionEncryptionKeyFactory {

  // Session encryption keys are AES
  static final String AES = "AES";

  // Key-derivation context has two parts: this static text
  // and the shared secret nonce.
  private static final byte[] CONTEXT_HEADER =
      Buffers.unwrap(StandardCharsets.US_ASCII.encode("AutomaticProvisioning-cipher"));

  private final byte[] kdfContext;

  SessionEncryptionKeyFactory(ByteBuffer sharedSecret) {

    this.kdfContext =
        Arrays.copyOf(CONTEXT_HEADER, CONTEXT_HEADER.length + sharedSecret.remaining());
    sharedSecret.get(this.kdfContext, CONTEXT_HEADER.length, sharedSecret.remaining());
  }

  abstract SecretKey build() throws NoSuchAlgorithmException, InvalidKeyException;

  byte[] getKdfContext() {
    return kdfContext;
  }
}
