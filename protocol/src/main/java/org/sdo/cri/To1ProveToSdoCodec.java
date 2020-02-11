// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.UUID;

class To1ProveToSdoCodec extends Codec<To1ProveToSdo> {

  private static final String AI = "ai";
  private static final String G2 = "g2";
  private static final String N4 = "n4";

  private final Codec<ByteBuffer> aiCodec = new AppIdCodec();
  private final Codec<UUID> g2Codec = new UuidCodec();

  @Override
  Codec<To1ProveToSdo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To1ProveToSdo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ByteBuffer> getAiCodec() {
    return aiCodec;
  }

  private Codec<UUID> getG2Codec() {
    return g2Codec;
  }

  private class Decoder extends Codec<To1ProveToSdo>.Decoder {

    @Override
    public To1ProveToSdo apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);

      expect(in, Json.asKey(AI));
      final ByteBuffer ai = getAiCodec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(N4));
      final Nonce n4 = new Nonce(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(G2));
      final UUID g2 = getG2Codec().decoder().apply(in);

      expect(in, Json.END_OBJECT);

      return new To1ProveToSdo(Buffers.unwrap(ai), n4, g2);
    }
  }

  private class Encoder extends Codec<To1ProveToSdo>.Encoder {

    @Override
    public void apply(Writer writer, To1ProveToSdo value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(AI));
      getAiCodec().encoder().apply(writer, ByteBuffer.wrap(value.getAi()));

      writer.write(Json.COMMA);
      writer.write(Json.asKey(N4));
      writer.write(value.getN4().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(G2));
      getG2Codec().encoder().apply(writer, value.getG2());

      writer.write(Json.END_OBJECT);
    }
  }
}
