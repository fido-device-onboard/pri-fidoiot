// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.UUID;

class To2SetupDeviceNohCodec extends Codec<To2SetupDeviceNoh> {

  private static final String G3 = "g3";
  private static final String N7 = "n7";
  private static final String R3 = "r3";

  private final Codec<UUID> g3Codec = new UuidCodec();
  private final Codec<RendezvousInfo> r3Codec = new RendezvousInfoCodec();

  @Override
  Codec<To2SetupDeviceNoh>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2SetupDeviceNoh>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<UUID> getG3Codec() {
    return g3Codec;
  }

  private Codec<RendezvousInfo> getR3Codec() {
    return r3Codec;
  }

  private class Decoder extends Codec<To2SetupDeviceNoh>.Decoder {

    @Override
    public To2SetupDeviceNoh apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(R3));
      final RendezvousInfo r3 = getR3Codec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(G3));
      final UUID g3 = getG3Codec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(N7));
      final Nonce n7 = new Nonce(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To2SetupDeviceNoh(r3, g3, n7);
    }
  }

  private class Encoder extends Codec<To2SetupDeviceNoh>.Encoder {

    @Override
    public void apply(Writer writer, To2SetupDeviceNoh value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(R3));
      getR3Codec().encoder().apply(writer, value.getR3());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(G3));
      getG3Codec().encoder().apply(writer, value.getG3());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(N7));
      writer.write(value.getN7().toString());

      writer.write(Json.END_OBJECT);
    }
  }
}
