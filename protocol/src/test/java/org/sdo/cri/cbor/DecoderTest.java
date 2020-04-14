package org.sdo.cri.cbor;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DecoderTest {

  static Stream<Collection<?>> testArray() {
    return Stream.of(
        List.of(),
        List.of(1L, 2L, 3L, 4L),
        List.of(1L, "two", List.of(3L, 4L)));
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName("arrays")
  void testArray(Collection<?> val) throws IOException {

    ByteArrayOutputStream cbor = new ByteArrayOutputStream();
    Encoder e = new Encoder.Builder(Channels.newChannel(cbor)).build();
    e.writeArray(val);

    Decoder d = new Decoder.Builder(
        Channels.newChannel(new ByteArrayInputStream(cbor.toByteArray())))
        .build();

    Object actual = d.next();
    assertEquals(val, actual);
  }
}