// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Json.BEGIN_OBJECT;
import static org.sdo.cri.Json.COMMA;
import static org.sdo.cri.Json.END_OBJECT;
import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class ErrorCodec extends Codec<Error> {

  private static final String EC = "ec";
  private static final String EM = "em";
  private static final String EMSG = "emsg";

  private final Codec<ErrorCode> ecCodec = new ErrorCodeCodec();
  private final Codec<String> emCodec = new StringCodec();
  private final Codec<Number> emsgCodec = new Uint8Codec();

  @Override
  Codec<Error>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<Error>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ErrorCode> getEcCodec() {
    return ecCodec;
  }

  private Codec<String> getEmCodec() {
    return emCodec;
  }

  private Codec<Number> getEmsgCodec() {
    return emsgCodec;
  }

  private class Decoder extends Codec<Error>.Decoder {

    @Override
    public Error apply(CharBuffer in) throws IOException {

      expect(in, BEGIN_OBJECT);
      expect(in, Json.asKey(EC));
      final ErrorCode ec = getEcCodec().decoder().apply(in);

      expect(in, COMMA);
      expect(in, Json.asKey(EMSG));
      final Number emsg = getEmsgCodec().decoder().apply(in);

      expect(in, COMMA);
      expect(in, Json.asKey(EM));
      final String em = getEmCodec().decoder().apply(in);

      expect(in, END_OBJECT);

      return new Error(ec, emsg.intValue(), em);
    }
  }

  private class Encoder extends Codec<Error>.Encoder {

    @Override
    public void apply(Writer writer, Error value) throws IOException {

      writer.write(BEGIN_OBJECT);
      writer.write(Json.asKey(EC));
      getEcCodec().encoder().apply(writer, value.getEc());

      writer.write(COMMA);
      writer.write(Json.asKey(EMSG));
      getEmsgCodec().encoder().apply(writer, value.getEmsg());

      writer.write(COMMA);
      writer.write(Json.asKey(EM));
      getEmCodec().encoder().apply(writer, value.getEm());

      writer.write(END_OBJECT);
    }
  }
}
