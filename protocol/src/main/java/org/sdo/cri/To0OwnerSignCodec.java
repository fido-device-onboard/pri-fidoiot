// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import org.sdo.cri.To0OwnerSignTo0dCodec.To0dDecoder;
import org.sdo.cri.To0OwnerSignTo0dCodec.To0dEncoder;

abstract class To0OwnerSignCodec {

  private static final String TO0D = "to0d";
  private static final String TO1D = "to1d";

  static class Decoder implements ProtocolDecoder<To0OwnerSign> {

    private final To0dDecoder to0dDec = new To0dDecoder();
    private char[] lastTo1d = new char[0];

    @Override
    public To0OwnerSign decode(final CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);
      expect(in, Json.asKey(TO0D));
      final To0OwnerSignTo0d to0d = to0dDec.decode(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(TO1D));
      CharBuffer to1dBuf = in.asReadOnlyBuffer();
      final SignatureBlock to1d = new SignatureBlockCodec.Decoder(null).decode(in);
      to1dBuf.limit(in.position());
      lastTo1d = new char[to1dBuf.remaining()];
      to1dBuf.get(lastTo1d);

      expect(in, Json.END_OBJECT);

      return new To0OwnerSign(to0d, to1d);
    }

    public CharBuffer getLastDc() {
      return to0dDec.getLastDc();
    }

    /**
     * Return the last-seen 'to1d' text.
     */
    public CharBuffer getLastTo1d() {
      CharBuffer cbuf = CharBuffer.allocate(lastTo1d.length);
      cbuf.put(lastTo1d);
      cbuf.flip();
      return cbuf;
    }
  }

  static class Encoder implements ProtocolEncoder<To0OwnerSign> {

    private final SignatureBlockCodec.Encoder sgEncoder;

    public Encoder(SignatureBlockCodec.Encoder sgEncoder) {
      this.sgEncoder = sgEncoder;
    }

    @Override
    public void encode(Writer writer, To0OwnerSign value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(TO0D));
      new To0dEncoder().encode(writer, value.getTo0d());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(TO1D));
      sgEncoder.encode(writer, value.getTo1d());

      writer.write(Json.END_OBJECT);
    }
  }
}
