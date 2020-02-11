// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

class To2ProveOpHdrCodec extends Codec<To2ProveOpHdr> {

  private static String EB = "eB";
  private static String HMAC = "hmac";
  private static String N5 = "n5";
  private static String N6 = "n6";
  private static String OH = "oh";
  private static String SZ = "sz";
  private static String XA = "xA";
  private final Decoder decoder = new Decoder();
  private final Codec<SigInfo> ebCodec = new SigInfoCodec();
  private final Encoder encoder = new Encoder();
  private final Codec<Number> szCodec = new Uint32Codec();
  private final Codec<ByteBuffer> xaCodec = new KexParamCodec();

  @Override
  Codec<To2ProveOpHdr>.Decoder decoder() {
    return getDecoder();
  }

  @Override
  Codec<To2ProveOpHdr>.Encoder encoder() {
    return getEncoder();
  }

  private Decoder getDecoder() {
    return decoder;
  }

  private Encoder getEncoder() {
    return encoder;
  }

  private Codec<SigInfo> getEbCodec() {
    return ebCodec;
  }

  private Codec<Number> getSzCodec() {
    return szCodec;
  }

  private Codec<ByteBuffer> getXaCodec() {
    return xaCodec;
  }

  class Decoder extends Codec<To2ProveOpHdr>.Decoder {

    private char[] lastHmac = new char[0];
    private char[] lastOh = new char[0];

    private final OwnershipVoucherHeaderCodec.OwnershipProxyHeaderDecoder ohDec =
        new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderDecoder();

    @Override
    public To2ProveOpHdr apply(CharBuffer in) throws IOException {

      Matchers.expect(in, Json.BEGIN_OBJECT);

      Matchers.expect(in, Json.asKey(SZ));
      final Number sz = getSzCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(OH));
      CharBuffer ohBuf = in.asReadOnlyBuffer();
      final OwnershipVoucherHeader oh = ohDec.decode(in);
      ohBuf.limit(in.position());
      setLastOh(ohBuf.asReadOnlyBuffer());

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(HMAC));
      CharBuffer hmacBuf = in.asReadOnlyBuffer();
      final HashMac hmac = new HashMac(in);
      hmacBuf.limit(in.position());
      setLastHmac(hmacBuf.asReadOnlyBuffer());

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(N5));
      final Nonce n5 = new Nonce(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(N6));
      final Nonce n6 = new Nonce(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(EB));
      final SigInfo eb = getEbCodec().decoder().apply(in);

      Matchers.expect(in, Json.COMMA);
      Matchers.expect(in, Json.asKey(XA));
      final ByteBuffer xa = getXaCodec().decoder().apply(in);

      Matchers.expect(in, Json.END_OBJECT);

      return new To2ProveOpHdr(sz.intValue(), oh, hmac, n5, n6, eb, xa);
    }

    public CharBuffer getLastD() {
      return ohDec.getLastD();
    }

    public CharBuffer getLastG() {
      return ohDec.getLastG();
    }

    /**
     * Return the last-seen 'hmac' text.
     */
    public CharBuffer getLastHmac() {
      CharBuffer dst = CharBuffer.allocate(lastHmac.length);
      dst.put(lastHmac);
      dst.flip();
      return dst;
    }

    private void setLastHmac(final CharBuffer src) {
      final char[] chars = new char[src.remaining()];
      src.get(chars);
      this.lastHmac = chars;
    }

    /**
     * Return the last-seen 'oh' text.
     */
    public CharBuffer getLastOh() {
      CharBuffer dst = CharBuffer.allocate(lastOh.length);
      dst.put(lastOh);
      dst.flip();
      return dst;
    }

    private void setLastOh(final CharBuffer src) {
      final char[] chars = new char[src.remaining()];
      src.get(chars);
      this.lastOh = chars;
    }
  }

  class Encoder extends Codec<To2ProveOpHdr>.Encoder {

    @Override
    public void apply(Writer writer, To2ProveOpHdr value) throws IOException {

      writer.write(Json.BEGIN_OBJECT);

      writer.write(Json.asKey(SZ));
      getSzCodec().encoder().apply(writer, value.getSz());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(OH));
      new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderEncoder().encode(writer, value.getOh());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(HMAC));
      writer.write(value.getHmac().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(N5));
      writer.write(value.getN5().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(N6));
      writer.write(value.getN6().toString());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(EB));
      getEbCodec().encoder().apply(writer, value.getEb());

      writer.write(Json.COMMA);
      writer.write(Json.asKey(XA));
      getXaCodec().encoder().apply(writer, value.getXa());

      writer.write(Json.END_OBJECT);
    }
  }
}
