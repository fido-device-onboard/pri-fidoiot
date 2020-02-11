// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

class Aes256KeyFactory extends SessionEncryptionKeyFactory {

  Aes256KeyFactory(ByteBuffer sharedSecret) {
    super(sharedSecret);
  }

  SecretKey build() throws NoSuchAlgorithmException, InvalidKeyException {

    KeyMaterialFactory keyMaterial1Factory = new KeyMaterialFactory(
        1, "HmacSHA384", ByteBuffer.wrap(getKdfContext()).asReadOnlyBuffer());

    byte[] keyMaterial1 = keyMaterial1Factory.build();
    return new SecretKeySpec(Arrays.copyOfRange(keyMaterial1, 0, 32), AES);
  }
}
