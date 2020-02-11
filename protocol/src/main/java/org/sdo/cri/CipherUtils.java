// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.InvalidKeyException;
import java.text.ParseException;
import org.slf4j.LoggerFactory;

class CipherUtils {

  /**
   * Decipher and decode an encrypted message in a single step.
   */
  public static <T> T decipherAndDecode(
      String encryptedMessage,
      EncryptedMessageCodec encryptedMessageCodec,
      ProtocolCipher cipher,
      Codec<T>.Decoder decoder) throws

      HmacVerificationException,
      InvalidKeyException,
      IOException,
      ParseException {

    return decipherAndDecode(encryptedMessage, encryptedMessageCodec, cipher, decoder::apply);
  }

  /**
   * Decipher and decode an encrypted message in a single step.
   */
  public static <T> T decipherAndDecode(
      String encryptedMessage,
      EncryptedMessageCodec encryptedMessageCodec,
      ProtocolCipher cipher,
      ProtocolDecoder<T> decoder) throws

      HmacVerificationException,
      InvalidKeyException,
      IOException,
      ParseException {

    CipherText113a ct = encryptedMessageCodec.decode(encryptedMessage);
    byte[] plainAscii = cipher.decipher(ct);
    CharBuffer plainText = US_ASCII.decode(ByteBuffer.wrap(plainAscii));
    LoggerFactory.getLogger(CipherUtils.class).info(plainText.asReadOnlyBuffer().toString());
    return decoder.decode(plainText);
  }

  /**
   * Encode and encipher a message in a single step.
   */
  public static <T> String encodeAndEncipher(
      T o,
      Codec<T>.Encoder encoder,
      ProtocolCipher cipher,
      EncryptedMessageCodec encryptedMessageCodec) throws

      InvalidKeyException,
      IOException {

    StringWriter stringWriter = new StringWriter();
    encoder.apply(stringWriter, o);
    LoggerFactory.getLogger(CipherUtils.class).info(stringWriter.toString());
    CipherText113a ct = cipher.encipher(stringWriter.toString().getBytes(US_ASCII));
    return encryptedMessageCodec.encode(ct);
  }
}
