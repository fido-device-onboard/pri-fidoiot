// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Codec for {@link DiAppStart}.
 */
class DiAppStartCodec extends Codec<DiAppStart> {

  private static final String M = "m";

  private final Codec<String> devMarkCodec = new StringCodec();

  @Override
  Codec<DiAppStart>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<DiAppStart>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<String> getMCodec() {
    return devMarkCodec;
  }

  private class Decoder extends Codec<DiAppStart>.Decoder {

    @Override
    public DiAppStart apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);

      expect(in, Json.asKey(M));
      String m = getMCodec().decoder().apply(in);

      expect(in, Json.END_OBJECT);

      return new DiAppStart(m);
    }
  }

  private class Encoder extends Codec<DiAppStart>.Encoder {

    @Override
    public void apply(Writer writer, DiAppStart value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(M));
      getMCodec().encoder().apply(writer, value.getM());

      writer.write(Json.END_OBJECT);
    }
  }
}
