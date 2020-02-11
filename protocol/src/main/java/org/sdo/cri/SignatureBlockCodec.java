// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.PublicKey;
import java.util.function.Consumer;

class SignatureBlockCodec {

  private static final String BO = "bo";
  private static final String PK = "pk";
  private static final String SG = "sg";

  private static Codec<ByteBuffer> getSgCodec() {
    return new ByteArrayCodec();
  }

  private static Codec<Number> getSgLenCodec() {
    return new Uint16Codec();
  }

  static class Decoder implements ProtocolDecoder<SignatureBlock> {

    private final Consumer<CharBuffer> hashFn;

    public Decoder(Consumer<CharBuffer> hashFn) {
      this.hashFn = hashFn;
    }

    @Override
    public SignatureBlock decode(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);
      Matchers.expect(in, Json.asKey(BO));

      // the body must be a JSON object
      final CharBuffer bo = in.asReadOnlyBuffer();
      Matchers.expect(in, Json.BEGIN_OBJECT);

      // loop until all outstanding braces close
      int depth = 1;

      while (depth > 0) {
        char c = in.get();

        if (Json.BEGIN_OBJECT == c) {
          ++depth;

        } else if (Json.END_OBJECT == c) {
          --depth;
        }
      }
      bo.limit(in.position());

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(PK));
      CharBuffer pkBuf = in.asReadOnlyBuffer();
      final PublicKey pk = new PublicKeyCodec.Decoder().decode(in);
      pkBuf.limit(in.position());

      if (null != hashFn) {
        hashFn.accept(pkBuf);
      }

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(SG));

      Matchers.expect(in, Json.BEGIN_ARRAY);
      final long sglen = getSgLenCodec().decoder().apply(in).longValue();

      Matchers.expect(in, Json.COMMA);
      ByteBuffer sg = getSgCodec().decoder().apply(in);
      if (sg.remaining() != sglen) {
        throw new IOException("sg length mismatch");
      }

      Matchers.expect(in, Json.END_ARRAY);
      Matchers.expect(in, Json.END_OBJECT);

      return new SignatureBlock(bo, pk, Buffers.unwrap(sg));
    }
  }

  static class Encoder implements ProtocolEncoder<SignatureBlock> {

    private final PublicKeyCodec.Encoder publicKeyEncoder;

    public Encoder(PublicKeyCodec.Encoder publicKeyEncoder) {
      this.publicKeyEncoder = publicKeyEncoder;
    }

    @Override
    public void encode(Writer writer, SignatureBlock value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(BO));
      writer.append(value.getBo());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(PK));
      publicKeyEncoder.encode(writer, value.getPk());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(SG));

      ByteBuffer sg = ByteBuffer.wrap(value.getSg());

      writer.write(Json.BEGIN_ARRAY);
      getSgLenCodec().encoder().apply(writer, sg.remaining());

      writer.write(Json.COMMA);
      getSgCodec().encoder().apply(writer, sg);

      writer.write(Json.END_ARRAY);
      writer.write(Json.END_OBJECT);
    }
  }
}
