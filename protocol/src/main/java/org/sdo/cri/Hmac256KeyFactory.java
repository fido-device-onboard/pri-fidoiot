// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

class Hmac256KeyFactory extends SessionVerificationKeyFactory {

  private static final String HMACSHA256 = "HmacSHA256";

  Hmac256KeyFactory(ByteBuffer sharedSecret) {
    super(sharedSecret);
  }

  SecretKey build() throws NoSuchAlgorithmException, InvalidKeyException {

    KeyMaterialFactory keyMaterial2Factory = new KeyMaterialFactory(
        2, HMACSHA256, ByteBuffer.wrap(getKdfContext()).asReadOnlyBuffer());

    byte[] keyMaterial2 = keyMaterial2Factory.build();
    return new SecretKeySpec(Arrays.copyOfRange(keyMaterial2, 0, 32), HMACSHA256);
  }
}
