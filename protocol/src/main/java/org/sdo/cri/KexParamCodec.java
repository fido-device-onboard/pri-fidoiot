// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Codec for SDO "KeyExchange/Key Exchange Parameter (KexParam)" type.
 *
 * @see "SDO Protocol Specification, 1.12k, 3.2: Composite Types"
 */
class KexParamCodec extends Codec<ByteBuffer> {

  private final Codec<ByteBuffer> dataCodec = new ByteArrayCodec();
  private final Codec<Number> lengthCodec = new Uint16Codec();

  @Override
  Codec<ByteBuffer>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<ByteBuffer>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ByteBuffer> getDataCodec() {
    return dataCodec;
  }

  private Codec<Number> getLengthCodec() {
    return lengthCodec;
  }

  private class Decoder extends Codec<ByteBuffer>.Decoder {

    @Override
    ByteBuffer apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_ARRAY);

      int len = getLengthCodec().decoder().apply(in).intValue();

      Matchers.expect(in, Json.COMMA);
      ByteBuffer value = getDataCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_ARRAY);

      if (len != value.remaining()) {
        throw new IOException("length mismatch");
      }

      return value;
    }
  }

  private class Encoder extends Codec<ByteBuffer>.Encoder {

    @Override
    void apply(Writer writer, ByteBuffer value) throws IOException {

      writer.write(Json.BEGIN_ARRAY);
      getLengthCodec().encoder().apply(writer, value.remaining());

      writer.append(Json.COMMA);
      getDataCodec().encoder().apply(writer, value);

      writer.write(Json.END_ARRAY);
    }
  }
}
