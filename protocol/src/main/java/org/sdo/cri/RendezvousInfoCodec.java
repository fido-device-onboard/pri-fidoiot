// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class RendezvousInfoCodec extends Codec<RendezvousInfo> {

  private final Codec<Number> lengthCodec = new Uint8Codec();

  @Override
  Codec<RendezvousInfo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<RendezvousInfo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getLengthCodec() {
    return lengthCodec;
  }

  private class Decoder extends Codec<RendezvousInfo>.Decoder {

    @Override
    public RendezvousInfo apply(CharBuffer in) throws IOException {

      RendezvousInfo value = new RendezvousInfo();

      Matchers.expect(in, Json.BEGIN_ARRAY);

      Long length = getLengthCodec().decoder().apply(in).longValue();

      for (Long l = 0L; l < length; ++l) {
        Matchers.expect(in, Json.COMMA);
        value.add(new RendezvousInstr(in));
      }

      Matchers.expect(in, Json.END_ARRAY);

      return value;
    }
  }

  private class Encoder extends Codec<RendezvousInfo>.Encoder {

    @Override
    public void apply(Writer writer, RendezvousInfo value) throws IOException {

      writer.write(Json.BEGIN_ARRAY);

      getLengthCodec().encoder().apply(writer, value.size());

      for (RendezvousInstr rvi : value) {
        writer.write(Json.COMMA);
        writer.write(rvi.toString());
      }

      writer.write(Json.END_ARRAY);
    }
  }
}
