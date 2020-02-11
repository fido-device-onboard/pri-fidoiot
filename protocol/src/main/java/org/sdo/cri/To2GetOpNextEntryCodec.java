// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To2GetOpNextEntryCodec extends Codec<To2GetOpNextEntry> {

  private static final String ENN = "enn";

  private final Codec<Number> ennCodec = new Uint32Codec();

  @Override
  Codec<To2GetOpNextEntry>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2GetOpNextEntry>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getEnnCodec() {
    return ennCodec;
  }

  private class Decoder extends Codec<To2GetOpNextEntry>.Decoder {

    @Override
    public To2GetOpNextEntry apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);
      Matchers.expect(in, Json.asKey(ENN));
      Number enn = getEnnCodec().decoder().apply(in);
      Matchers.expect(in, Json.END_OBJECT);

      return new To2GetOpNextEntry(enn.intValue());
    }
  }

  private class Encoder extends Codec<To2GetOpNextEntry>.Encoder {

    @Override
    public void apply(Writer writer, To2GetOpNextEntry value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(ENN));
      getEnnCodec().encoder().apply(writer, value.getEnn());
      writer.write(Json.END_OBJECT);
    }
  }
}

