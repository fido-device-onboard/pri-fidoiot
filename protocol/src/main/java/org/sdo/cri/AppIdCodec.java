// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Codec for AppID.
 *
 * @see "SDO Protocol Specification, 1.12k, 3.2: Composite Types"
 */
class AppIdCodec extends Codec<ByteBuffer> {

  private final Codec<ByteBuffer> appIdBytesCodec = new ByteArrayCodec();
  private final Codec<Number> lengthCodec = new Uint8Codec();
  private final Codec<Number> typeCodec = new Uint8Codec();

  @Override
  Codec<ByteBuffer>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<ByteBuffer>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ByteBuffer> getAppIdBytesCodec() {
    return appIdBytesCodec;
  }

  private Codec<Number> getLengthCodec() {
    return lengthCodec;
  }

  private Codec<Number> getTypeCodec() {
    return typeCodec;
  }

  private class Decoder extends Codec<ByteBuffer>.Decoder {

    @Override
    ByteBuffer apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_ARRAY);
      final int length = getLengthCodec().decoder().apply(in).intValue();

      Matchers.expect(in, Json.COMMA);
      getTypeCodec().decoder().apply(in); // unused, see spec 3.2.5

      Matchers.expect(in, Json.COMMA);
      final ByteBuffer appIdBytes = getAppIdBytesCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_ARRAY);

      if (appIdBytes.remaining() != length) {
        throw new IOException("length mismatch");
      }

      return appIdBytes;
    }
  }

  private class Encoder extends Codec<ByteBuffer>.Encoder {

    @Override
    void apply(Writer writer, ByteBuffer value) throws IOException {

      writer.write(Json.BEGIN_ARRAY);
      getLengthCodec().encoder().apply(writer, value.remaining());

      writer.write(Json.COMMA);
      getTypeCodec().encoder().apply(writer, 0); // unused, see spec 3.2.5

      writer.write(Json.COMMA);
      getAppIdBytesCodec().encoder().apply(writer, value);

      writer.write(Json.END_ARRAY);
    }
  }
}
