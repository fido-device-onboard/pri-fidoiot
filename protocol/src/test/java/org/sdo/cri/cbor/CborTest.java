package org.sdo.cri.cbor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CborTest {

  static Stream<Collection<?>> testArray() {
    return Stream.of(
        List.of(),
        List.of(1L, 2L, 3L, 4L),
        List.of(1L, "two", List.of(3L, 4L)));
  }

  static Stream<BigInteger> testBigNum() {
    BigInteger i = BigInteger.probablePrime(Long.SIZE + 1, ThreadLocalRandom.current());
    return Stream.of(
        BigInteger.ZERO,
        i
        // At the time of this writing, Jackson-CBOR (2.10.3) has an off-by-one bug
        // in its encoding of negative bignums.  We can't test against a broken reference!
        // , i.negate()
    );
  }

  static Stream<byte[]> testBytes() {
    return Stream.of(
        new byte[]{},
        new byte[]{0},
        new byte[]{1, 2, 3, 4}
    );
  }

  static Stream<Long> testInt() {
    return Stream.of(
        0L,
        AdditionalInfo.MAXINT,
        -AdditionalInfo.MAXINT,
        (long) Byte.MIN_VALUE,
        (long) Byte.MAX_VALUE,
        (long) Short.MIN_VALUE,
        (long) Short.MAX_VALUE,
        (long) Integer.MIN_VALUE,
        (long) Integer.MAX_VALUE,
        Long.MIN_VALUE,
        Long.MAX_VALUE);
  }

  static Stream<Map<?, ?>> testMap() {
    return Stream.of(
        Map.of(),
        Map.of(1L, 2L, 3L, 4L),
        Map.of(1L, "two", "three", Map.of(4L, 5L)));
  }

  static Stream<String> testText() {
    return Stream.of(
        "",
        "CBOR",
        "ü",
        "水",
        "\ud800\udd51"
    );
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName("arrays")
  void testArray(Collection<?> val) throws IOException {

    ByteArrayOutputStream cbor = new ByteArrayOutputStream();
    Encoder e = new Encoder.Builder(Channels.newChannel(cbor)).build();
    e.writeArray(val);

    CBORFactory f = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(f);

    byte[] expected = mapper.writeValueAsBytes(val);
    assertArrayEquals(expected, cbor.toByteArray());

    Decoder d = new Decoder.Builder(
        Channels.newChannel(new ByteArrayInputStream(cbor.toByteArray())))
        .build();

    Object val2 = d.next();
    assertEquals(val, val2);
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName("BigNums")
  void testBigNum(BigInteger val) throws IOException {

    ByteArrayOutputStream cbor = new ByteArrayOutputStream();
    Encoder e = new Encoder.Builder(Channels.newChannel(cbor)).build();

    e.writeBigNum(val);

    CBORFactory f = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(f);

    byte[] expected = mapper.writeValueAsBytes(val);
    assertArrayEquals(expected, cbor.toByteArray());

    Decoder d = new Decoder.Builder(
        Channels.newChannel(new ByteArrayInputStream(cbor.toByteArray())))
        .build();

    Object val2 = d.next();
    assertEquals(val, val2);
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName("byte strings")
  void testBytes(byte[] val) throws IOException {

    ByteArrayOutputStream cbor = new ByteArrayOutputStream();
    Encoder e = new Encoder.Builder(Channels.newChannel(cbor)).build();

    e.writeBytes(ByteBuffer.wrap(val));

    CBORFactory f = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(f);

    byte[] expected = mapper.writeValueAsBytes(val);
    assertArrayEquals(expected, cbor.toByteArray());

    Decoder d = new Decoder.Builder(
        Channels.newChannel(new ByteArrayInputStream(cbor.toByteArray())))
        .build();

    Object val2 = d.next();
    assertEquals(ByteBuffer.wrap(val), val2);
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName("integers")
  void testInt(long val) throws IOException {

    ByteArrayOutputStream cbor = new ByteArrayOutputStream();
    Encoder e = new Encoder.Builder(Channels.newChannel(cbor)).build();

    e.writeLong(val);

    CBORFactory f = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(f);

    byte[] expected = mapper.writeValueAsBytes(val);
    assertArrayEquals(expected, cbor.toByteArray());

    Decoder d = new Decoder.Builder(
        Channels.newChannel(new ByteArrayInputStream(cbor.toByteArray())))
        .build();

    Object val2 = d.next();
    assertEquals(val, val2);
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName("maps")
  void testMap(Map<?, ?> val) throws IOException {

    ByteArrayOutputStream cbor = new ByteArrayOutputStream();
    Encoder e = new Encoder.Builder(Channels.newChannel(cbor)).build();
    e.writeMap(val);

    // At this writing, Jackson does not support finite-length maps via ObjectMapper.
    // We can, however, test that it decodes to a matching object.
    // Jackson also reads all map keys as strings, so we must compare string representations,
    // not data values.
    CBORFactory f = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(f);
    Map<?, ?> actual = mapper.readValue(cbor.toByteArray(), Map.class);

    assertEquals(val.toString(), actual.toString());

    Decoder d = new Decoder.Builder(
        Channels.newChannel(new ByteArrayInputStream(cbor.toByteArray())))
        .build();

    Object val2 = d.next();
    assertEquals(val, val2);
  }

  @Test
  @DisplayName("null")
  void testNull() throws IOException {
    ByteArrayOutputStream cbor = new ByteArrayOutputStream();
    Encoder e = new Encoder.Builder(Channels.newChannel(cbor)).build();

    e.writeNull();

    CBORFactory f = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(f);

    byte[] expected = mapper.writeValueAsBytes(null);
    assertArrayEquals(expected, cbor.toByteArray());

    Decoder d = new Decoder.Builder(
        Channels.newChannel(new ByteArrayInputStream(cbor.toByteArray())))
        .build();

    Object val2 = d.next();
    assertEquals(null, val2);
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName("text strings")
  void testText(String val) throws IOException {
    ByteArrayOutputStream cbor = new ByteArrayOutputStream();
    Encoder e = new Encoder.Builder(Channels.newChannel(cbor)).build();

    e.writeText(CharBuffer.wrap(val));

    CBORFactory f = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(f);

    byte[] expected = mapper.writeValueAsBytes(val);
    assertArrayEquals(expected, cbor.toByteArray());

    Decoder d = new Decoder.Builder(
        Channels.newChannel(new ByteArrayInputStream(cbor.toByteArray())))
        .build();

    Object val2 = d.next();
    assertEquals(val, val2);
  }
}