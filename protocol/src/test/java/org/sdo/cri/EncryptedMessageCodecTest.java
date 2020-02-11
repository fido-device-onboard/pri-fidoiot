// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class EncryptedMessageCodecTest {

  private final CipherText113a ct = new CipherText113a(new byte[]{0}, new byte[]{0, 0});

  static SecretKey buildSecretKey() {
    byte[] bytes = new byte[32];
    ThreadLocalRandom.current().nextBytes(bytes);
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Test
  void test() throws Exception {
    final EncryptedMessageCodec emc = new EncryptedMessageCodec(buildSecretKey());
    String encoded = emc.encode(ct);
    CipherText113a decoded = emc.decode(encoded);
    assertEquals(ct, decoded);
  }

  @Test
  void testParseError() throws Exception {
    final EncryptedMessageCodec emc = new EncryptedMessageCodec(buildSecretKey());
    String encoded = emc.encode(ct);
    assertThrows(ParseException.class, () -> emc.decode(encoded.replace(':', 'y')));
  }

  @Test
  void testHmacError() throws Exception {
    EncryptedMessageCodec emc = new EncryptedMessageCodec(buildSecretKey());
    String encoded = emc.encode(ct);
    CipherText113a decoded = emc.decode(encoded);
    assertThrows(
        HmacVerificationException.class,
        () -> new EncryptedMessageCodec(buildSecretKey()).decode(encoded));
  }
}
