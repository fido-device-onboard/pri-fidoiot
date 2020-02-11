// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.UUID;

class To2HelloDeviceCodec extends Codec<To2HelloDevice> {

  private static String CS = "cs";
  private static String EA = "eA";
  private static String G2 = "g2";
  private static String KX = "kx";
  private static String N5 = "n5";
  private static String PE = "pe";

  private final Codec<CipherType> csCodec = new CipherTypeCodec();
  private final Codec<SigInfo> eaCodec = new SigInfoCodec();
  private final Codec<UUID> g2Codec = new UuidCodec();
  private final Codec<KeyExchangeType> kxCodec = new KeyExchangeTypeCodec();
  private final Codec<KeyEncoding> peCodec = new KeyEncodingCodec();

  @Override
  Codec<To2HelloDevice>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<To2HelloDevice>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<CipherType> getCsCodec() {
    return csCodec;
  }

  private Codec<SigInfo> getEaCodec() {
    return eaCodec;
  }

  private Codec<UUID> getG2Codec() {
    return g2Codec;
  }

  private Codec<KeyExchangeType> getKxCodec() {
    return kxCodec;
  }

  private Codec<KeyEncoding> getPeCodec() {
    return peCodec;
  }

  private class Decoder extends Codec<To2HelloDevice>.Decoder {

    @Override
    public To2HelloDevice apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(G2));
      final UUID g2 = getG2Codec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(N5));
      final Nonce n5 = new Nonce(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(PE));
      final KeyEncoding pe = getPeCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(KX));
      final KeyExchangeType kx = getKxCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(CS));
      final CipherType cs = getCsCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(EA));
      final SigInfo ea = getEaCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To2HelloDevice(g2, n5, pe, kx, cs, ea);
    }
  }

  private class Encoder extends Codec<To2HelloDevice>.Encoder {

    @Override
    public void apply(Writer writer, To2HelloDevice value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(G2));
      getG2Codec().encoder().apply(writer, value.getG2());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(N5));
      writer.write(value.getN5().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(PE));
      getPeCodec().encoder().apply(writer, value.getPe());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(KX));
      getKxCodec().encoder().apply(writer, value.getKx());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(CS));
      getCsCodec().encoder().apply(writer, value.getCs());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(EA));
      getEaCodec().encoder().apply(writer, value.getEa());

      writer.write(Json.END_OBJECT);
    }
  }
}
