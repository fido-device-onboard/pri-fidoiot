// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

class To2OpNextEntryCodec {

  private static final String ENI = "eni";
  private static final String ENN = "enn";

  private static Codec<Number> getEnnCodec() {
    return new Uint32Codec();
  }

  static class Decoder implements ProtocolDecoder<To2OpNextEntry> {

    @Override
    public To2OpNextEntry decode(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(ENN));
      final Number enn = getEnnCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(ENI));
      final SignatureBlock eni = new SignatureBlockCodec.Decoder(null).decode(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To2OpNextEntry(enn.intValue(), eni);
    }
  }

  static class Encoder implements ProtocolEncoder<To2OpNextEntry> {

    private final SignatureBlockCodec.Encoder eniEncoder;

    public Encoder(SignatureBlockCodec.Encoder eniEncoder) {
      this.eniEncoder = eniEncoder;
    }

    @Override
    public void encode(Writer writer, To2OpNextEntry value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(ENN));
      getEnnCodec().encoder().apply(writer, value.getEnn());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(ENI));
      eniEncoder.encode(writer, value.getEni());

      writer.write(Json.END_OBJECT);
    }
  }
}
