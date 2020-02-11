// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.UUID;

class To1HelloSdoCodec extends Codec<To1HelloSdo> {

  private static final String EA = "eA";
  private static final String G2 = "g2";

  private final Codec<SigInfo> eaCodec = new SigInfoCodec();
  private final Codec<UUID> g2Codec = new UuidCodec();

  @Override
  Codec<To1HelloSdo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To1HelloSdo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<SigInfo> getEaCodec() {
    return eaCodec;
  }

  private Codec<UUID> getG2Codec() {
    return g2Codec;
  }

  private class Decoder extends Codec<To1HelloSdo>.Decoder {

    @Override
    public To1HelloSdo apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);

      expect(in, Json.asKey(G2));
      final UUID g2 = getG2Codec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(EA));
      SigInfo ea = getEaCodec().decoder().apply(in);

      expect(in, Json.END_OBJECT);

      return new To1HelloSdo(g2, ea);
    }
  }


  private class Encoder extends Codec<To1HelloSdo>.Encoder {

    @Override
    public void apply(Writer writer, To1HelloSdo value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(G2));
      getG2Codec().encoder().apply(writer, value.getG2());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(EA));
      getEaCodec().encoder().apply(writer, value.getEa());

      writer.write(Json.END_OBJECT);
    }
  }
}
