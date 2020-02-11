// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DiffieHellmanKeyExchangeTest {

  private static SecureRandom secureRandom;

  @BeforeAll
  static void beforeAll() throws Exception {
    secureRandom = SecureRandom.getInstance("SHA1PRNG");
  }

  @Test
  void dh14_generateSharedSecret_secretsMatch() throws Exception {

    KeyExchange kxa = new DiffieHellmanKeyExchange.Group14(secureRandom);
    KeyExchange kxb = new DiffieHellmanKeyExchange.Group14(secureRandom);

    ByteBuffer shSeA = kxa.generateSharedSecret(kxb.getMessage());
    ByteBuffer shSeB = kxb.generateSharedSecret(kxa.getMessage());
    assertEquals(shSeA, shSeB);
  }

  @Test
  void dh15_generateSharedSecret_secretsMatch() throws Exception {

    KeyExchange kxa = new DiffieHellmanKeyExchange.Group15(secureRandom);
    KeyExchange kxb = new DiffieHellmanKeyExchange.Group15(secureRandom);

    ByteBuffer shSeA = kxa.generateSharedSecret(kxb.getMessage());
    ByteBuffer shSeB = kxb.generateSharedSecret(kxa.getMessage());
    assertEquals(shSeA, shSeB);
  }
}
