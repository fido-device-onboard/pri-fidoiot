// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To2Done2Codec extends Codec<To2Done2> {

  private static final String N7 = "n7";

  @Override
  Codec<To2Done2>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2Done2>.Encoder encoder() {
    return new Encoder();
  }

  private class Decoder extends Codec<To2Done2>.Decoder {

    @Override
    public To2Done2 apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);

      expect(in, Json.asKey(N7));
      final Nonce n7 = new Nonce(in);

      expect(in, Json.END_OBJECT);

      return new To2Done2(n7);
    }
  }

  private class Encoder extends Codec<To2Done2>.Encoder {

    @Override
    public void apply(Writer writer, To2Done2 value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(N7));
      writer.write(value.getN7().toString());

      writer.write(Json.END_OBJECT);
    }
  }
}

