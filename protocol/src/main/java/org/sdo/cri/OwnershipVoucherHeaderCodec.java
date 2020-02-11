// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;
import java.security.PublicKey;
import java.util.UUID;

/**
 * Codec for {@link OwnershipVoucherHeader}.
 */
abstract class OwnershipVoucherHeaderCodec {

  private static final String D = "d";
  private static final String G = "g";
  private static final String HDC = "hdc";
  private static final String PE = "pe";
  private static final String PK = "pk";
  private static final String PV = "pv";
  private static final String R = "r";

  static class OwnershipProxyHeaderDecoder implements
      ProtocolDecoder<OwnershipVoucherHeader> {

    private char[] lastD = new char[0];
    private char[] lastG = new char[0];
    private char[] lastPk = new char[0];

    @Override
    public OwnershipVoucherHeader decode(final CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);
      expect(in, Json.asKey(PV));
      final Version pv =
          Version.valueOfInt(new Uint32Codec().decoder().apply(in).intValue());
      if (OwnershipVoucherHeader.THIS_VERSION.intValue() != pv.intValue()) {
        // version mismatch, we can't parse this.
        throw new IOException("version mismatch, pv = " + pv);
      }

      expect(in, Json.COMMA);
      expect(in, Json.asKey(PE));
      final KeyEncoding pe = new KeyEncodingCodec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(R));
      final RendezvousInfo r = new RendezvousInfoCodec().decoder().apply(in);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(G));
      CharBuffer gbuf = in.asReadOnlyBuffer();
      final UUID g = new UuidCodec().decoder().apply(in);
      gbuf.limit(in.position());
      setLastG(gbuf);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(D));
      CharBuffer dbuf = in.asReadOnlyBuffer();
      final String d = new StringCodec().decoder().apply(in);
      dbuf.limit(in.position());
      setLastD(dbuf);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(PK));
      CharBuffer pkBuf = in.asReadOnlyBuffer();
      final PublicKey pk = new PublicKeyCodec.Decoder().decode(in);
      pkBuf.limit(in.position());
      setLastPk(pkBuf);

      HashDigest hdc;
      in.mark();

      try {
        expect(in, Json.COMMA);
        expect(in, Json.asKey(HDC));
        hdc = new HashDigest(in);

      } catch (BufferUnderflowException | IOException e) {
        in.reset(); // hdc not present in this header
        hdc = null;
      }

      expect(in, Json.END_OBJECT);

      return new OwnershipVoucherHeader(pe, r, g, d, pk, hdc);
    }

    /**
     * Return the last-seen 'd' text.
     */
    public CharBuffer getLastD() {
      CharBuffer dst = CharBuffer.allocate(lastD.length);
      dst.put(lastD);
      dst.flip();
      return dst;
    }

    private void setLastD(final CharBuffer src) {
      final char[] chars = new char[src.remaining()];
      src.get(chars);
      this.lastD = chars;
    }

    /**
     * Return the last-seen 'g' text.
     */
    public CharBuffer getLastG() {
      CharBuffer dst = CharBuffer.allocate(lastG.length);
      dst.put(lastG);
      dst.flip();
      return dst;
    }

    private void setLastG(final CharBuffer src) {
      final char[] chars = new char[src.remaining()];
      src.get(chars);
      this.lastG = chars;
    }

    /**
     * Return the last-seen 'pk' text.
     */
    public CharBuffer getLastPk() {
      CharBuffer dst = CharBuffer.allocate(lastPk.length);
      dst.put(lastPk);
      dst.flip();
      return dst;
    }

    private void setLastPk(final CharBuffer src) {
      final char[] chars = new char[src.remaining()];
      src.get(chars);
      this.lastPk = chars;
    }
  }

  static class OwnershipProxyHeaderEncoder implements
      ProtocolEncoder<OwnershipVoucherHeader> {

    @Override
    public void encode(Writer out, OwnershipVoucherHeader val) throws IOException {

      out.write(Json.BEGIN_OBJECT);

      out.write(Json.asKey(PV));
      new Uint32Codec().encoder().apply(out, val.getPv().intValue());

      out.write(Json.COMMA);
      out.write(Json.asKey(PE));
      new KeyEncodingCodec().encoder().apply(out, val.getPe());

      out.write(Json.COMMA);
      out.write(Json.asKey(R));
      new RendezvousInfoCodec().encoder().apply(out, val.getR());

      out.write(Json.COMMA);
      out.write(Json.asKey(G));
      new UuidCodec().encoder().apply(out, val.getG());

      out.write(Json.COMMA);
      out.write(Json.asKey(D));
      new StringCodec().encoder().apply(out, val.getD());

      out.write(Json.COMMA);
      out.write(Json.asKey(PK));
      new PublicKeyCodec.Encoder(val.getPe()).encode(out, val.getPk());

      HashDigest hdc = val.getHdc();

      if (null != hdc) {
        out.write(Json.COMMA);
        out.write(Json.asKey(HDC));
        out.write(hdc.toString());
      }

      out.write(Json.END_OBJECT);
    }
  }
}
