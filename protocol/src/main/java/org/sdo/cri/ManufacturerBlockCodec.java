// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Codec for {@link ManufacturerBlock}.
 */
class ManufacturerBlockCodec extends Codec<ManufacturerBlock> {

  private static final String D = "d";

  private final Codec<String> deviceInfoCodec = new StringCodec();

  @Override
  Codec<ManufacturerBlock>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<ManufacturerBlock>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<String> getDCodec() {
    return deviceInfoCodec;
  }

  private class Decoder extends Codec<ManufacturerBlock>.Decoder {

    @Override
    public ManufacturerBlock apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(D));
      final String d = getDCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new ManufacturerBlock(d);
    }
  }

  private class Encoder extends Codec<ManufacturerBlock>.Encoder {

    @Override
    public void apply(Writer writer, ManufacturerBlock value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(D));
      getDCodec().encoder().apply(writer, value.getD());

      writer.write(Json.END_OBJECT);
    }
  }
}
