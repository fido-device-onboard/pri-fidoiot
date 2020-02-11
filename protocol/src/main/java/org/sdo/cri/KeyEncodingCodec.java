// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Codec for {@link KeyEncoding}.
 */
class KeyEncodingCodec extends Codec<KeyEncoding> {

  private final Codec<Number> numberCodec = new Uint8Codec();

  @Override
  Codec<KeyEncoding>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<KeyEncoding>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getNumberCodec() {
    return numberCodec;
  }

  private class Decoder extends Codec<KeyEncoding>.Decoder {

    @Override
    public KeyEncoding apply(CharBuffer in) throws IOException {

      return KeyEncoding.fromNumber(getNumberCodec().decoder().apply(in));
    }
  }

  private class Encoder extends Codec<KeyEncoding>.Encoder {

    @Override
    public void apply(Writer writer, KeyEncoding value) throws IOException {
      getNumberCodec().encoder().apply(writer, value.toInteger());
    }
  }
}
