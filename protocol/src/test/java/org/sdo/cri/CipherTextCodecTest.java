// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.CharBuffer;
import java.text.ParseException;
import org.junit.jupiter.api.Test;

class CipherTextCodecTest {

  private final String encoded = "[[1,\"AA==\"],2,\"AAA=\"]";
  private final CipherText113a decoded = new CipherText113a(new byte[]{0}, new byte[]{0, 0});

  @Test
  void testDecode() throws Exception {
    CipherText113a c = CipherTextCodec.decode(CharBuffer.wrap(encoded));
    assertEquals(decoded, c);
  }

  @Test
  void testDecodeError() {
    assertThrows(
        ParseException.class,
        () -> CipherTextCodec.decode(CharBuffer.wrap(encoded.replace(',', 'y'))));
  }

  @Test
  void testEncode() {
    CipherText113a c = decoded;
    String actual = CipherTextCodec.encode(c);
    assertEquals(encoded, actual);
  }
}
