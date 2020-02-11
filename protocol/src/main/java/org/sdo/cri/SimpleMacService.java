// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * A simple implementation of {@link MacService}.
 */
class SimpleMacService implements MacService {

  private final SecretKey key;

  public SimpleMacService(SecretKey key) {
    this.key = key;
  }

  public SimpleMacService(byte[] key) {
    this(buildKey(key));
  }

  private static SecretKey buildKey(byte[] keyBytes) {
    if (Objects.requireNonNull(keyBytes).length <= 256) {
      return new SecretKeySpec(keyBytes, "HmacSHA256");
    } else {
      return new SecretKeySpec(keyBytes, "HmacSHA384");
    }
  }

  @Override
  public HashMac macOf(final ByteBuffer... ins) {

    final Mac mac;

    try {
      mac = Mac.getInstance(key.getAlgorithm(), BouncyCastleLoader.load());
      mac.init(key);

    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    for (final ByteBuffer in : ins) {
      mac.update(in);
    }

    return new HashMac(mac.doFinal());
  }
}
