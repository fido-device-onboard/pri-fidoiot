// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class ErrorCodeCodec extends Codec<ErrorCode> {

  private final Codec<Number> numberCodec = new Uint16Codec();

  @Override
  Codec<ErrorCode>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<ErrorCode>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<Number> getNumberCodec() {
    return numberCodec;
  }

  private class Decoder extends Codec<ErrorCode>.Decoder {

    @Override
    public ErrorCode apply(CharBuffer in) throws IOException {

      return ErrorCode.fromNumber(getNumberCodec().decoder().apply(in));
    }
  }

  private class Encoder extends Codec<ErrorCode>.Encoder {

    @Override
    public void apply(Writer writer, ErrorCode value) throws IOException {
      getNumberCodec().encoder().apply(writer, value.toInteger());
    }
  }
}
