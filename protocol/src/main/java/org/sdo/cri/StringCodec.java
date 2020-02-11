// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class StringCodec extends Codec<String> {

  @Override
  Codec<String>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<String>.Encoder encoder() {
    return new Encoder();
  }

  private class Decoder extends Codec<String>.Decoder {

    @Override
    public String apply(CharBuffer in) throws IOException {
      return Strings.decode(in);
    }
  }

  private class Encoder extends Codec<String>.Encoder {

    @Override
    public void apply(Writer writer, String value) throws IOException {
      writer.write(Strings.encode(value));
    }
  }
}
