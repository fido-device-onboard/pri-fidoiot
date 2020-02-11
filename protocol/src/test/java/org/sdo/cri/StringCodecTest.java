// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringCodecTest {

  private StringCodec stringCodec;

  @BeforeAll
  static void beforeAll() {

  }

  @BeforeEach
  void beforeEach() {
    stringCodec = new StringCodec();
  }

  String buildTestString() {

    StringBuilder builder = new StringBuilder();
    byte[] b = new byte[1];

    for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
      b[0] = (byte)i;
      builder.append(US_ASCII.decode(ByteBuffer.wrap(b)));
    }

    return builder.toString();
  }

  @Test
  void encoderApply_complexCharacters_escapesOutput() throws IOException {
    String raw = buildTestString();
    StringWriter writer = new StringWriter();
    stringCodec.encoder().apply(writer, raw);
    String encoded = writer.toString();
    Assertions.assertNotEquals(raw, encoded);
  }

  @Test
  void decoderApply_escapedInput_decodesEscapes() throws IOException {
    String raw = buildTestString();
    StringWriter writer = new StringWriter();
    stringCodec.encoder().apply(writer, raw);
    String encoded = writer.toString();

    String decoded = stringCodec.decoder().apply(CharBuffer.wrap(encoded));
    Assertions.assertEquals(raw, decoded);
  }
}
