// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.time.Duration;

abstract class To0OwnerSignTo0dCodec {

  private static final String N3 = "n3";
  private static final String OP = "op";
  private static final String WS = "ws";

  static class To0dDecoder implements ProtocolDecoder<To0OwnerSignTo0d> {

    private final OwnershipVoucherCodec.OwnershipProxyDecoder opDec =
        new OwnershipVoucherCodec.OwnershipProxyDecoder();

    @Override
    public To0OwnerSignTo0d decode(final CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);
      Matchers.expect(in, Json.asKey(OP));
      final OwnershipVoucher113 op = opDec.decode(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(WS));
      final Duration ws = Duration.ofSeconds(new Uint32Codec().decoder().apply(in).longValue());

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(N3));
      final Nonce n3 = new Nonce(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To0OwnerSignTo0d(op, ws, n3);
    }

    CharBuffer getLastDc() {
      return opDec.getLastDc();
    }
  }

  static class To0dEncoder implements ProtocolEncoder<To0OwnerSignTo0d> {

    @Override
    public void encode(final Writer writer, final To0OwnerSignTo0d value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);
      writer.write(Json.asKey(OP));
      new OwnershipVoucherCodec.OwnershipProxyEncoder().encode(writer, value.getOp());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(WS));
      new Uint32Codec().encoder().apply(writer, value.getWs().getSeconds());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(N3));
      writer.write(value.getN3().toString());

      writer.write(Json.END_OBJECT);
    }
  }
}
