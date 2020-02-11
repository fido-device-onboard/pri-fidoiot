// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CtrCipherTest {

  @BeforeAll
  static void beforeAll() {
    Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);

    if (null == provider) {
      provider = new BouncyCastleProvider();
      Security.insertProviderAt(provider, 1);
    }
  }

  @Test
  void test() throws Exception {

    byte[] plainText = "the quick brown fox jumped over the lazy dogs".getBytes();
    byte[] aesKeyBytes = new byte[32];

    ThreadLocalRandom.current().nextBytes(aesKeyBytes);

    SecretKeySpec sek = new SecretKeySpec(aesKeyBytes, "AES");
    CtrCipher c = new CtrCipher(sek, SecureRandom.getInstance("SHA1PRNG"));
    CipherText113a ct = c.encipher(plainText);
    assertArrayEquals(plainText, c.decipher(ct));

    byte[] iv = ct.getIv();
    byte[] mutatedIv = Arrays.copyOf(iv, iv.length);
    mutatedIv[0] += 1;
    CipherText113a mutatedCt = new CipherText113a(mutatedIv, ct.getCt());
    assertFalse(Arrays.equals(plainText, c.decipher(mutatedCt)));
  }
}
