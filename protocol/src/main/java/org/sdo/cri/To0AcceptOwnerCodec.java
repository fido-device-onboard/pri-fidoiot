// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.time.Duration;

class To0AcceptOwnerCodec extends Codec<To0AcceptOwner> {

  private static final String WS = "ws";

  private final Codec<Number> wsCodec = new Uint32Codec();

  @Override
  Codec<To0AcceptOwner>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To0AcceptOwner>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getWsCodec() {
    return wsCodec;
  }

  private class Decoder extends Codec<To0AcceptOwner>.Decoder {

    @Override
    public To0AcceptOwner apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(WS));
      Duration ws = Duration.ofSeconds(getWsCodec().decoder().apply(in).longValue());

      Matchers.expect(in, Json.END_OBJECT);

      return new To0AcceptOwner(ws);
    }
  }

  private class Encoder extends Codec<To0AcceptOwner>.Encoder {

    @Override
    public void apply(Writer writer, To0AcceptOwner value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(WS));
      getWsCodec().encoder().apply(writer, value.getWs().getSeconds());

      writer.write(Json.END_OBJECT);
    }
  }
}
