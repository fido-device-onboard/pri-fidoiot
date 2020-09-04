// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.cbor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.Pipe;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EncoderTest {

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testBool(boolean val) throws IOException {
    Pipe p = Pipe.open();
    Encoder e = new Encoder.Builder(p.sink()).build();
    Decoder d = new Decoder.Builder(p.source()).build();

    e.writeBoolean(val);
    assertTrue(d.hasNext());
    Object o = d.next();
    assertEquals(val, o);
  }

  @Test
  void testMap() throws IOException {
    Pipe p = Pipe.open();
    Encoder e = new Encoder.Builder(p.sink()).build();
    Decoder d = new Decoder.Builder(p.source()).build();

    Map<Long, String> first = Map.of(1L, "one", 2L, "two");
    e.writeMap(first);
    assertTrue(d.hasNext());
    Map<Object, Object> second = (Map<Object, Object>) d.next();

    assertTrue(first.entrySet().stream()
        .allMatch(en -> en.getValue().equals(second.get(en.getKey()))));
  }

  @Test
  void testNegativeBigNum() throws IOException {

    Pipe p = Pipe.open();
    Encoder e = new Encoder.Builder(p.sink()).build();
    Decoder d = new Decoder.Builder(p.source()).build();

    BigInteger i = new BigInteger("-1234567890abcdef1234567890abcdef1234567890abcdef", 16);
    e.writeBigNum(i);
    assertTrue(d.hasNext());
    Object o = d.next();
    assertEquals(i, o);
  }

  @Test
  void testNull() throws IOException {
    Pipe p = Pipe.open();
    Encoder e = new Encoder.Builder(p.sink()).build();
    Decoder d = new Decoder.Builder(p.source()).build();

    e.writeNull();
    assertTrue(d.hasNext());
    Object o = d.next();
    assertEquals(null, o);
  }

  @Test
  void testPositiveBigNum() throws IOException {

    Pipe p = Pipe.open();
    Encoder e = new Encoder.Builder(p.sink()).build();
    Decoder d = new Decoder.Builder(p.source()).build();

    BigInteger i = new BigInteger("1234567890abcdef1234567890abcdef1234567890abcdef", 16);
    e.writeBigNum(i);
    assertTrue(d.hasNext());
    Object o = d.next();
    assertEquals(i, o);
  }
}
