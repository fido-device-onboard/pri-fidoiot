// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To1HelloSdoAckCodec extends Codec<To1HelloSdoAck> {

  private static final String EB = "eB";
  private static final String N4 = "n4";
  private final Codec<SigInfo> ebCodec = new SigInfoCodec();

  @Override
  Codec<To1HelloSdoAck>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To1HelloSdoAck>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<SigInfo> getEbCodec() {
    return ebCodec;
  }

  private class Decoder extends Codec<To1HelloSdoAck>.Decoder {

    @Override
    public To1HelloSdoAck apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(N4));
      final Nonce n4 = new Nonce(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(EB));
      final SigInfo eb = getEbCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To1HelloSdoAck(n4, eb);
    }
  }

  private class Encoder extends Codec<To1HelloSdoAck>.Encoder {

    @Override
    public void apply(Writer writer, To1HelloSdoAck value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(N4));
      writer.write(value.getN4().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(EB));
      getEbCodec().encoder().apply(writer, value.getEb());

      writer.write(Json.END_OBJECT);
    }
  }
}
