// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To0HelloAckCodec extends Codec<To0HelloAck> {

  private static final String N3 = "n3";

  @Override
  Codec<To0HelloAck>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To0HelloAck>.Encoder encoder() {
    return new Encoder();
  }

  private class Decoder extends Codec<To0HelloAck>.Decoder {

    @Override
    public To0HelloAck apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(N3));
      Nonce n3 = new Nonce(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To0HelloAck(n3);
    }
  }

  private class Encoder extends Codec<To0HelloAck>.Encoder {

    @Override
    public void apply(Writer writer, To0HelloAck value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(N3));
      writer.write(value.getN3().toString());

      writer.write(Json.END_OBJECT);
    }
  }
}
