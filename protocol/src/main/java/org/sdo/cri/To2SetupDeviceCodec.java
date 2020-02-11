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

class To2SetupDeviceCodec {

  private static final String NOH = "noh";
  private static final String OSINN = "osinn";

  private static Codec<Number> getOsinnCodec() {
    return new Uint32Codec();
  }

  static class Decoder implements ProtocolDecoder<To2SetupDevice> {

    private final SignatureBlockCodec.Decoder sgDecoder;

    public Decoder(SignatureBlockCodec.Decoder sgDecoder) {
      this.sgDecoder = sgDecoder;
    }

    @Override
    public To2SetupDevice decode(CharBuffer in) throws IOException {

      expect(in, BEGIN_OBJECT);

      expect(in, Json.asKey(OSINN));
      final Number osinn = getOsinnCodec().decoder().apply(in);

      expect(in, COMMA);
      expect(in, Json.asKey(NOH));
      final SignatureBlock noh = sgDecoder.decode(in);

      expect(in, END_OBJECT);

      return new To2SetupDevice(osinn.intValue(), noh);
    }
  }

  static class Encoder implements ProtocolEncoder<To2SetupDevice> {

    private final SignatureBlockCodec.Encoder nohEncoder;

    public Encoder(SignatureBlockCodec.Encoder nohEncoder) {
      this.nohEncoder = nohEncoder;
    }

    @Override
    public void encode(Writer writer, To2SetupDevice value) throws IOException {

      writer.write(BEGIN_OBJECT);

      writer.write(Json.asKey(OSINN));
      getOsinnCodec().encoder().apply(writer, value.getOsinn());

      writer.write(COMMA);
      writer.write(Json.asKey(NOH));
      nohEncoder.encode(writer, value.getNoh());

      writer.write(END_OBJECT);
    }
  }

}
