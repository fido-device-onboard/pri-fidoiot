// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To2DoneCodec extends Codec<To2Done> {

  private static final String HMAC = "hmac";
  private static final String N6 = "n6";

  @Override
  Codec<To2Done>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2Done>.Encoder encoder() {
    return new Encoder();
  }

  private class Decoder extends Codec<To2Done>.Decoder {

    @Override
    public To2Done apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(HMAC));
      final HashMac hmac = new HashMac(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(N6));
      final Nonce n6 = new Nonce(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To2Done(hmac, n6);
    }
  }

  private class Encoder extends Codec<To2Done>.Encoder {

    @Override
    public void apply(Writer writer, To2Done value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(HMAC));
      writer.write(value.getHmac().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(N6));
      writer.write(value.getN6().toString());

      writer.write(Json.END_OBJECT);
    }
  }
}

