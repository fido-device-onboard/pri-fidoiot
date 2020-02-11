// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Codec for {@link SignatureType}.
 */
class SignatureTypeCodec extends Codec<SignatureType> {

  private final Codec<Number> codec = new Uint8Codec();

  private Codec<Number> getCodec() {
    return codec;
  }

  @Override
  Codec<SignatureType>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<SignatureType>.Encoder encoder() {
    return new Encoder();
  }

  private class Decoder extends Codec<SignatureType>.Decoder {

    @Override
    public SignatureType apply(CharBuffer in) throws IOException {
      return SignatureType.fromNumber(getCodec().decoder().apply(in));
    }
  }

  private class Encoder extends Codec<SignatureType>.Encoder {

    @Override
    public void apply(Writer writer, SignatureType value) throws IOException {
      getCodec().encoder().apply(writer, value.toInteger());
    }
  }
}
