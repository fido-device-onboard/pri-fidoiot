// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

class PkRmeCodec extends Codec<PublicKey> {

  private final Codec<ByteBuffer> dataCodec = new ByteArrayCodec();
  private final Codec<Number> lengthCodec = new Uint32Codec();

  @Override
  Codec<PublicKey>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<PublicKey>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<ByteBuffer> getDataCodec() {
    return dataCodec;
  }

  private Codec<Number> getLengthCodec() {
    return lengthCodec;
  }

  private class Decoder extends Codec<PublicKey>.Decoder {

    @Override
    public PublicKey apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_ARRAY);
      final long modBytes = getLengthCodec().decoder().apply(in).longValue();

      Matchers.expect(in, Json.COMMA);
      final ByteBuffer mod = getDataCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      long expBytes = getLengthCodec().decoder().apply(in).longValue();

      Matchers.expect(in, Json.COMMA);
      final ByteBuffer exp = getDataCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_ARRAY);

      if (!(mod.remaining() == modBytes && exp.remaining() == expBytes)) {
        throw new IOException("length mismatch");
      }

      try {
        return Keys.toPublicKey(new RSAPublicKeySpec(
            new BigInteger(1, Buffers.unwrap(mod)),
            new BigInteger(1, Buffers.unwrap(exp))));

      } catch (NoSuchAlgorithmException e) {
        throw new IOException(e);
      }
    }
  }

  private class Encoder extends Codec<PublicKey>.Encoder {

    @Override
    public void apply(Writer writer, PublicKey value) throws IOException {

      if (!(value instanceof RSAPublicKey)) {
        throw new IllegalArgumentException(value.getClass().getCanonicalName());
      }

      RSAPublicKey key = (RSAPublicKey) value;
      final ByteBuffer mod = ByteBuffer.wrap(key.getModulus().toByteArray());
      final ByteBuffer exp = ByteBuffer.wrap(key.getPublicExponent().toByteArray());

      writer.write(Json.BEGIN_ARRAY);
      getLengthCodec().encoder().apply(writer, mod.remaining());

      writer.write(Json.COMMA);
      getDataCodec().encoder().apply(writer, mod);

      writer.write(Json.COMMA);
      getLengthCodec().encoder().apply(writer, exp.remaining());

      writer.write(Json.COMMA);
      getDataCodec().encoder().apply(writer, exp);

      writer.write(Json.END_ARRAY);
    }
  }
}
