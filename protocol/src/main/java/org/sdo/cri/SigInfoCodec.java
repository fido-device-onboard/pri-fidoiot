// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Codec for {@link SigInfo}.
 */
class SigInfoCodec extends Codec<SigInfo> {

  private final Codec<ByteBuffer> infoCodec = new ByteArrayCodec();
  private final Codec<Number> lengthCodec = new Uint16Codec();
  private final Codec<SignatureType> sgTypeCodec = new SignatureTypeCodec();

  @Override
  Codec<SigInfo>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<SigInfo>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ByteBuffer> getInfoCodec() {
    return infoCodec;
  }

  private Codec<Number> getLengthCodec() {
    return lengthCodec;
  }

  private Codec<SignatureType> getSgTypeCodec() {
    return sgTypeCodec;
  }

  private class Decoder extends Codec<SigInfo>.Decoder {

    @Override
    public SigInfo apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_ARRAY);
      final SignatureType sgType = getSgTypeCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      final int length = getLengthCodec().decoder().apply(in).intValue();

      Matchers.expect(in, Json.COMMA);
      final ByteBuffer info = getInfoCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_ARRAY);

      if (info.remaining() != length) {
        throw new IOException("length mismatch");
      }

      return new SigInfo(sgType, info);
    }
  }

  private class Encoder extends Codec<SigInfo>.Encoder {

    @Override
    public void apply(Writer writer, SigInfo value) throws IOException {

      writer.write(Json.BEGIN_ARRAY);

      getSgTypeCodec().encoder().apply(writer, value.getSgType());

      writer.write(Json.COMMA);
      ByteBuffer info = value.getInfo();
      getLengthCodec().encoder().apply(writer, info.remaining());

      writer.write(Json.COMMA);
      getInfoCodec().encoder().apply(writer, value.getInfo());

      writer.write(Json.END_ARRAY);
    }
  }
}
