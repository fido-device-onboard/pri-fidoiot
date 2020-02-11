// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * Codec for SDO TO2 'Encrypted Messages', per SDO 1.13a 4.5.
 */
class EncryptedMessageCodec implements Serializable {

  private static final String CT_HEADER = "{\"ct\":";
  private static final String HMAC_HEADER = ",\"hmac\":[";
  private static final String FOOTER = "]}";

  private final SecretKey svk;

  /**
   * Constructs a new codec using the given Session Validation Key (SVK).
   *
   * @param svk the session validation (HMAC) key.
   */
  public EncryptedMessageCodec(SecretKey svk) {
    this.svk = svk;
  }

  /**
   * Decodes an encoded Encrypted Message as a {@link CipherText113a} object.
   *
   * @param in the encrypted message text.
   * @return the decoded {@link CipherText113a}.
   * @throws HmacVerificationException if the HMAC verification fails.
   * @throws InvalidKeyException       if the codec's SVK is invalid.
   * @throws ParseException            if the encrypted message is not parsable.
   */
  public CipherText113a decode(CharSequence in) throws
      HmacVerificationException,
      InvalidKeyException,
      ParseException {

    CharBuffer inBuf = CharBuffer.wrap(in);

    if (!Matchers.consumeMatching(CT_HEADER, inBuf)) {
      throw new ParseException(in.toString(), inBuf.position());
    }

    final CharBuffer ctBuf = inBuf.duplicate();
    final CipherText113a ct = CipherTextCodec.decode(inBuf);
    ctBuf.limit(inBuf.position());

    final byte[] actualHmac;
    try {
      Mac mac = Mac.getInstance(macAlgorithm(), BouncyCastleLoader.load());
      mac.init(svk);
      actualHmac = mac.doFinal(ctBuf.toString().getBytes(StandardCharsets.US_ASCII));
    } catch (NoSuchAlgorithmException e) {
      // bug smell: we shouldn't be asking for unavailable mac algorithms
      throw new RuntimeException("PROBABLE BUG!", e);
    }

    if (!Matchers.consumeMatching(HMAC_HEADER, inBuf)) {
      throw new ParseException(in.toString(), inBuf.position());
    }

    final int nhmac;
    try {
      nhmac = new Uint8Codec().decoder().apply(inBuf).intValue();
    } catch (IOException e) {
      throw new ParseException(in.toString(), inBuf.position());
    }

    if (!Matchers.consumeMatching(Json.COMMA.toString(), inBuf)) {
      throw new ParseException(in.toString(), inBuf.position());
    }

    Codec<ByteBuffer>.Decoder byteArrayDecoder = new ByteArrayCodec().decoder();
    ByteBuffer bbuf;
    try {
      bbuf = byteArrayDecoder.apply(inBuf);
    } catch (IOException e) {
      throw new ParseException(in.toString(), inBuf.position());
    }

    if (bbuf.remaining() != nhmac) {
      throw new ParseException(in.toString(), inBuf.position());
    }
    final byte[] expectedHmac = new byte[bbuf.remaining()];
    bbuf.get(expectedHmac);

    if (!Matchers.consumeMatching(FOOTER, inBuf)) {
      throw new ParseException(in.toString(), inBuf.position());
    }

    if (!Arrays.equals(expectedHmac, actualHmac)) {
      throw new HmacVerificationException();
    }

    return ct;
  }

  /**
   * Encode the given {@link CipherText113a} as an SDO Encrypted Message.
   *
   * @param cipherText the input {@link CipherText113a}.
   * @return The encoded message text.
   * @throws InvalidKeyException If the codec's SVK is invalid.
   */
  public String encode(CipherText113a cipherText) throws InvalidKeyException {
    StringWriter stringWriter = new StringWriter();
    Codec<ByteBuffer>.Encoder byteArrayEncoder = new ByteArrayCodec().encoder();
    String ct = CipherTextCodec.encode(cipherText);

    final byte[] hmac;
    try {
      Mac mac = Mac.getInstance(macAlgorithm(), BouncyCastleLoader.load());
      mac.init(svk);
      hmac = mac.doFinal(ct.getBytes(StandardCharsets.US_ASCII));
    } catch (NoSuchAlgorithmException e) {
      // bug smell: we shouldn't be asking for unavailable mac algorithms
      throw new RuntimeException("PROBABLE BUG!", e);
    }

    try {
      stringWriter.append(CT_HEADER)
          .append(ct)
          .append(HMAC_HEADER);

      new Uint8Codec().encoder().apply(stringWriter, hmac.length);

      stringWriter.append(Json.COMMA);
      byteArrayEncoder.apply(stringWriter, ByteBuffer.wrap(hmac));

      stringWriter.append(FOOTER);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return stringWriter.toString();
  }

  // Compute the JSE Mac algorithm based on the SVK size.
  private String macAlgorithm() {
    switch (svk.getEncoded().length) {
      case 32:
        return "HmacSHA256";
      case 64:
        return "HmacSHA384";
      default:
        throw new UnsupportedOperationException("illegal SVK length: " + svk.getEncoded().length);
    }
  }
}
