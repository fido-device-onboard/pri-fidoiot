// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * A simple implementation of {@link DigestService}.
 */
class SimpleDigestService implements DigestService {

  private final DigestType digestType;

  SimpleDigestService(final DigestType digestType) {
    this.digestType = Objects.requireNonNull(digestType);
  }

  @Override
  public HashDigest digestOf(final ByteBuffer[] ins) {

    final MessageDigest digest;
    try {
      digest =
          MessageDigest.getInstance(getDigestType().toJceAlgorithm(), BouncyCastleLoader.load());

      for (final ByteBuffer in : ins) {
        digest.update(in);
      }

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    return new HashDigest(getDigestType(), ByteBuffer.wrap(digest.digest()));
  }

  private DigestType getDigestType() {
    return digestType;
  }
}
