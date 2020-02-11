// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

class Hmac384KeyFactory extends SessionVerificationKeyFactory {

  private static final String HMACSHA384 = "HmacSHA384";

  Hmac384KeyFactory(ByteBuffer sharedSecret) {
    super(sharedSecret);
  }

  SecretKey build() throws NoSuchAlgorithmException, InvalidKeyException {

    KeyMaterialFactory keyMaterial2aFactory = new KeyMaterialFactory(
        2, HMACSHA384, ByteBuffer.wrap(getKdfContext()).asReadOnlyBuffer());
    KeyMaterialFactory keyMaterial2bFactory = new KeyMaterialFactory(
        3, HMACSHA384, ByteBuffer.wrap(getKdfContext()).asReadOnlyBuffer());

    byte[] keyMaterial2a = keyMaterial2aFactory.build();
    byte[] keyMaterial2b = keyMaterial2bFactory.build();

    byte[] combinedKeyMaterial = new byte[64];
    ByteBuffer bbuf = ByteBuffer.wrap(combinedKeyMaterial);
    bbuf.put(keyMaterial2a, 0, 48);
    bbuf.put(keyMaterial2b, 0, 16);

    return new SecretKeySpec(combinedKeyMaterial, HMACSHA384);
  }
}
