// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.ParseException;

/**
 * Codec for {@link CipherText113a} objects, per SDO 1.13a 4.5.
 */
class CipherTextCodec {

  /**
   * Decodes a character-encoded {@link CipherText113a} object.
   *
   * @param inBuf the encoded text.
   *
   * @return the decoded {@link CipherText113a} object.
   *
   * @throws ParseException if the input text was not parsable.
   */
  public static CipherText113a decode(CharBuffer inBuf) throws ParseException {

    if (!Matchers.consumeMatching(Json.BEGIN_ARRAY.toString(), inBuf)) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    if (!Matchers.consumeMatching(Json.BEGIN_ARRAY.toString(), inBuf)) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    Number ivlen;
    try {
      ivlen = new Uint8Codec().decoder().apply(inBuf);
    } catch (IOException e) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    if (!Matchers.consumeMatching(Json.COMMA.toString(), inBuf)) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    Codec<ByteBuffer>.Decoder byteArrayDecoder = new ByteArrayCodec().decoder();
    ByteBuffer bbuf;
    try {
      bbuf = byteArrayDecoder.apply(inBuf);
    } catch (IOException e) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    if (bbuf.remaining() != ivlen.intValue()) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    final byte[] iv = new byte[bbuf.remaining()];
    bbuf.get(iv);

    if (!Matchers.consumeMatching(Json.END_ARRAY.toString(), inBuf)) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    if (!Matchers.consumeMatching(Json.COMMA.toString(), inBuf)) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    final int nct;
    try {
      nct = new Uint16Codec().decoder().apply(inBuf).intValue();
    } catch (IOException e) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    if (!Matchers.consumeMatching(Json.COMMA.toString(), inBuf)) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    try {
      bbuf = byteArrayDecoder.apply(inBuf);
    } catch (IOException e) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    if (bbuf.remaining() != nct) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }
    final byte[] ct = new byte[bbuf.remaining()];
    bbuf.get(ct);

    if (!Matchers.consumeMatching(Json.END_ARRAY.toString(), inBuf)) {
      throw new ParseException(inBuf.toString(), inBuf.position());
    }

    return new CipherText113a(iv, ct);
  }

  /**
   * Encodes a {@link CipherText113a} object as a character string.
   *
   * @param cipherText the input {@link CipherText113a}.
   *
   * @return the encoded string.
   */
  public static String encode(CipherText113a cipherText) {
    StringWriter stringWriter = new StringWriter();
    Codec<ByteBuffer>.Encoder byteArrayEncoder = new ByteArrayCodec().encoder();

    final byte[] iv = cipherText.getIv();
    final byte[] ct = cipherText.getCt();

    try {
      stringWriter.append(Json.BEGIN_ARRAY);

      stringWriter.append(Json.BEGIN_ARRAY);
      new Uint8Codec().encoder().apply(stringWriter, iv.length);
      stringWriter.append(Json.COMMA);
      byteArrayEncoder.apply(stringWriter, ByteBuffer.wrap(iv));
      stringWriter.append(Json.END_ARRAY);

      stringWriter.append(Json.COMMA);
      new Uint16Codec().encoder().apply(stringWriter, ct.length);

      stringWriter.append(Json.COMMA);
      byteArrayEncoder.apply(stringWriter, ByteBuffer.wrap(ct));

      stringWriter.append(Json.END_ARRAY);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return stringWriter.toString();
  }
}
