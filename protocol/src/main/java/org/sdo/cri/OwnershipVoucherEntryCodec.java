// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.security.PublicKey;

/**
 * Codec for {@link OwnershipVoucherEntry}.
 */
class OwnershipVoucherEntryCodec {

  private static final String HC = "hc";
  private static final String HP = "hp";
  private static final String PK = "pk";

  static class Decoder implements ProtocolDecoder<OwnershipVoucherEntry> {

    @Override
    public OwnershipVoucherEntry decode(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(HP));
      final HashDigest hp = new HashDigest(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(HC));
      final HashDigest hc = new HashDigest(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(PK));
      final PublicKey pk = new PublicKeyCodec.Decoder().decode(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new OwnershipVoucherEntry(hp, hc, pk);
    }
  }

  static class Encoder implements ProtocolEncoder<OwnershipVoucherEntry> {

    private final PublicKeyCodec.Encoder pkEncoder;

    public Encoder(PublicKeyCodec.Encoder pkEncoder) {
      this.pkEncoder = pkEncoder;
    }

    @Override
    public void encode(Writer writer, OwnershipVoucherEntry value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(HP));
      writer.write(value.getHp().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(HC));
      writer.write(value.getHc().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(PK));
      pkEncoder.encode(writer, value.getPk());

      writer.write(Json.END_OBJECT);
    }
  }
}
