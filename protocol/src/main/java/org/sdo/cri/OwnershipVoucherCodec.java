// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.Matchers.expect;

import java.io.IOException;
import java.io.Writer;
import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;
import java.security.cert.CertPath;
import java.util.LinkedList;
import java.util.List;

/**
 * Codec for {@link OwnershipVoucher113}.
 */
abstract class OwnershipVoucherCodec {

  private static final String HMAC = "hmac";
  private static final String OH = "oh";
  private static final String DC = "dc";
  private static final String EN = "en";
  private static final String SZ = "sz";

  static class OwnershipProxyDecoder implements ProtocolDecoder<OwnershipVoucher113> {

    private final Codec<CertPath>.Decoder dcDec = new CertPathCodec().decoder();
    private final OwnershipVoucherHeaderCodec.OwnershipProxyHeaderDecoder ohDec =
        new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderDecoder();
    private final Codec<Number>.Decoder szDec = new Uint32Codec().decoder();

    private char[] lastDc = new char[0];
    private char[] lastHmac = new char[0];
    private char[] lastOh = new char[0];

    @Override
    public OwnershipVoucher113 decode(final CharBuffer in) throws IOException {

      expect(in, Json.BEGIN_OBJECT);

      expect(in, Json.asKey(SZ));
      final int sz = szDec.apply(in).intValue();

      expect(in, Json.COMMA);
      expect(in, Json.asKey(OH));
      CharBuffer cbuf = in.asReadOnlyBuffer();
      final OwnershipVoucherHeader oh = ohDec.decode(in);
      cbuf.limit(in.position());
      lastOh = new char[cbuf.remaining()];
      cbuf.get(lastOh);

      expect(in, Json.COMMA);
      expect(in, Json.asKey(HMAC));
      cbuf = in.asReadOnlyBuffer();
      final HashMac hmac = new HashMac(in);
      cbuf.limit(in.position());
      lastHmac = new char[cbuf.remaining()];
      cbuf.get(lastHmac);

      CertPath dc;
      in.mark();

      try {
        expect(in, Json.COMMA);
        expect(in, Json.asKey(DC));
        cbuf = in.duplicate();
        dc = dcDec.apply(in);
        cbuf.limit(in.position());
        lastDc = new char[cbuf.remaining()];
        cbuf.get(lastDc);

      } catch (BufferUnderflowException | IOException e) {
        in.reset(); // dc not present in this proxy
        dc = null;
      }

      expect(in, Json.COMMA);
      expect(in, Json.asKey(EN));
      expect(in, Json.BEGIN_ARRAY);

      Character separator = null;
      List<SignatureBlock> en = new LinkedList<>();
      SignatureBlockCodec.Decoder enDec = new SignatureBlockCodec.Decoder(null);
      for (int n = 0; n < sz; n++) {

        if (null != separator) {
          expect(in, separator);

        } else {
          separator = Json.COMMA;
        }

        en.add(enDec.decode(in));
      }

      expect(in, Json.END_ARRAY);
      expect(in, Json.END_OBJECT);

      return new OwnershipVoucher113(oh, hmac, dc, en);
    }

    public CharBuffer getLastD() {
      return ohDec.getLastD();
    }

    /**
     * Return the last-seen 'dc' text, suitable for hashing.
     */
    public CharBuffer getLastDc() {
      CharBuffer cbuf = CharBuffer.allocate(lastDc.length);
      cbuf.put(lastDc);
      cbuf.flip();
      return cbuf;
    }

    /**
     * Return the last-seen 'g' text.
     */
    public CharBuffer getLastG() {
      return ohDec.getLastG();
    }

    /**
     * Return the last-seen 'hmac' text.
     */
    public CharBuffer getLastHmac() {
      CharBuffer cbuf = CharBuffer.allocate(lastHmac.length);
      cbuf.put(lastHmac);
      cbuf.flip();
      return cbuf;
    }

    /**
     * Return the last-seen 'oh' text.
     */
    public CharBuffer getLastOh() {
      CharBuffer cbuf = CharBuffer.allocate(lastOh.length);
      cbuf.put(lastOh);
      cbuf.flip();
      return cbuf;
    }
  }

  static class OwnershipProxyEncoder implements ProtocolEncoder<OwnershipVoucher113> {

    private final Codec<CertPath>.Encoder dcEnc = new CertPathCodec().encoder();
    private final Codec<Number>.Encoder szEnc = new Uint32Codec().encoder();

    @Override
    public void encode(final Writer out, final OwnershipVoucher113 val) throws IOException {

      out.write(Json.BEGIN_OBJECT);

      out.write(Json.asKey(SZ));
      szEnc.apply(out, val.getEn().size());

      out.write(Json.COMMA);
      out.write(Json.asKey(OH));
      new OwnershipVoucherHeaderCodec.OwnershipProxyHeaderEncoder().encode(out, val.getOh());

      out.write(Json.COMMA);
      out.write(Json.asKey(HMAC));
      out.write(val.getHmac().toString());

      CertPath dc = val.getDc();

      if (null != dc) {
        out.write(Json.COMMA);
        out.write(Json.asKey(DC));
        dcEnc.apply(out, dc);
      }

      out.write(Json.COMMA);
      out.write(Json.asKey(EN));
      out.write(Json.BEGIN_ARRAY);

      Character separator = null;
      SignatureBlockCodec.Encoder enEnc =
          new SignatureBlockCodec.Encoder(new PublicKeyCodec.Encoder(val.getOh().getPe()));
      for (SignatureBlock en : val.getEn()) {

        if (null != separator) {
          out.write(separator);

        } else {
          separator = Json.COMMA;
        }

        enEnc.encode(out, en);
      }

      out.write(Json.END_ARRAY);
      out.write(Json.END_OBJECT);
    }
  }
}
