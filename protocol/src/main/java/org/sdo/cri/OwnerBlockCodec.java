// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.UUID;

/**
 * Codec for {@link OwnerBlock}.
 */
class OwnerBlockCodec extends Codec<OwnerBlock> {

  private static final String G = "g";
  private static final String PE = "pe";
  private static final String PKH = "pkh";
  private static final String PV = "pv";
  private static final String R = "r";

  private final Codec<UUID> guidCodec = new UuidCodec();
  private final Codec<KeyEncoding> peCodec = new KeyEncodingCodec();
  private final Codec<Number> pvCodec = new Uint32Codec();
  private final Codec<RendezvousInfo> rendezvousInfoCodec = new RendezvousInfoCodec();

  @Override
  Codec<OwnerBlock>.Decoder decoder() {
    return new Decoder();
  }

  @Override
  Codec<OwnerBlock>.Encoder encoder() {
    return new Encoder();
  }

  private Codec<UUID> getGCodec() {
    return guidCodec;
  }

  private Codec<KeyEncoding> getPeCodec() {
    return peCodec;
  }

  private Codec<Number> getPvCodec() {
    return pvCodec;
  }

  private Codec<RendezvousInfo> getRCodec() {
    return rendezvousInfoCodec;
  }

  private class Decoder extends Codec<OwnerBlock>.Decoder {

    @Override
    public OwnerBlock apply(CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);
      expect(in, Json.asKey(PV));
      Number pv = getPvCodec().decoder().apply(in);
      if (OwnerBlock.THIS_VERSION.intValue() != pv.intValue()) {
        // version mismatch, we can't parse this.
        throw new IOException("version mismatch, pv = " + pv);
      }

      expect(in, Json.COMMA);
      expect(in, Json.asKey(PE));
      final KeyEncoding pe = getPeCodec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(G));
      final UUID g = getGCodec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(R));
      final RendezvousInfo r = getRCodec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(PKH));
      final HashDigest pkh = new HashDigest(in);

      expect(in, Json.END_OBJECT);

      return new OwnerBlock(pe, g, r, pkh);
    }
  }

  private class Encoder extends Codec<OwnerBlock>.Encoder {

    @Override
    public void apply(Writer writer, OwnerBlock value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(PV));
      getPvCodec().encoder().apply(writer, value.getPv().intValue());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(PE));
      getPeCodec().encoder().apply(writer, value.getPe());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(G));
      getGCodec().encoder().apply(writer, value.getG());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(R));
      getRCodec().encoder().apply(writer, value.getR());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(PKH));
      writer.write(value.getPkh().toString());

      writer.write(Json.END_OBJECT);
    }
  }
}
