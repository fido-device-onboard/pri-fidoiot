// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To2GetNextOwnerServiceInfoCodec extends Codec<To2GetNextOwnerServiceInfo> {

  private static final String NN = "nn";

  private final Codec<Number> nnCodec = new Uint32Codec();

  @Override
  Codec<To2GetNextOwnerServiceInfo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2GetNextOwnerServiceInfo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getNnCodec() {
    return nnCodec;
  }

  private class Decoder extends Codec<To2GetNextOwnerServiceInfo>.Decoder {

    @Override
    public To2GetNextOwnerServiceInfo apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);
      expect(in, Json.asKey(NN));
      Number nn = getNnCodec().decoder().apply(in);
      expect(in, Json.END_OBJECT);

      return new To2GetNextOwnerServiceInfo(nn.intValue());
    }
  }

  private class Encoder extends Codec<To2GetNextOwnerServiceInfo>.Encoder {

    @Override
    public void apply(Writer writer, To2GetNextOwnerServiceInfo value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(NN));
      getNnCodec().encoder().apply(writer, value.getNn());
      writer.write(Json.END_OBJECT);
    }
  }
}
