// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Writer;
import java.nio.CharBuffer;

class To0HelloCodec extends Codec<To0Hello> {

  @Override
  Codec<To0Hello>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To0Hello>.Encoder encoder() {
    return new Encoder();
  }

  private class Decoder extends Codec<To0Hello>.Decoder {

    @Override
    public To0Hello apply(CharBuffer in) {
      return new To0Hello();
    }
  }

  private class Encoder extends Codec<To0Hello>.Encoder {

    @Override
    public void apply(Writer writer, To0Hello value) {
      // no body
    }
  }
}
