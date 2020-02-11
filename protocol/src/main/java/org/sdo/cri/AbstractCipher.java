// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Common implementation of all SDO TO2 ciphers.
 */
abstract class AbstractCipher implements ProtocolCipher, Serializable {

  private final SecretKey sek;
  private final SecureRandom mySecureRandom;

  /**
   * Construct a new cipher using the given Session Encryption Key.
   *
   * @param sek The Session Encryption Key (SEK).
   * @param secureRandom The SecureRandom providing randomness.
   */
  AbstractCipher(SecretKey sek, SecureRandom secureRandom) {
    this.sek = sek;
    this.mySecureRandom = secureRandom;
  }

  /**
   * Builds the initialization vector for the next encode operation.
   */
  void buildNextIv(byte[] dst) {
    mySecureRandom.nextBytes(dst);
  }

  /**
   * Returns the cipher transformation string to be used in
   * {@link Cipher#getInstance}.
   */
  protected abstract String cipherTransformation();

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] decipher(CipherText113a in) throws InvalidKeyException {

    final Cipher cipher;
    try {
      cipher = Cipher.getInstance(cipherTransformation(), BouncyCastleLoader.load());
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      // Another bug smell...
      throw new RuntimeException("PROBABLE BUG!", e);
    }

    IvParameterSpec ivParams = new IvParameterSpec(in.getIv());

    try {
      cipher.init(Cipher.DECRYPT_MODE, sek, ivParams);
    } catch (InvalidAlgorithmParameterException e) {
      // Another bug smell...
      throw new RuntimeException("PROBABLE BUG!", e);
    }

    try {
      return cipher.doFinal(in.getCt());
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      // Another bug smell...
      throw new RuntimeException("PROBABLE BUG!", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CipherText113a encipher(byte[] in) throws InvalidKeyException {

    final Cipher cipher;
    try {
      cipher = Cipher.getInstance(cipherTransformation(), BouncyCastleLoader.load());
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      // bug smell - there's no caller input which should produce this result
      // and expecting callers to handle these exceptions is unreasonable.
      throw new RuntimeException("PROBABLE BUG!", e);
    }

    final byte[] iv = new byte[cipher.getBlockSize()];
    buildNextIv(iv);

    try {
      cipher.init(Cipher.ENCRYPT_MODE, sek, new IvParameterSpec(iv));
    } catch (InvalidAlgorithmParameterException e) {
      // Another bug smell...
      throw new RuntimeException("PROBABLE BUG!", e);
    }

    try {
      return postEncipher(new CipherText113a(iv, cipher.doFinal(in)));
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      // Another bug smell...
      throw new RuntimeException("PROBABLE BUG!", e);
    }
  }

  /**
   * A hook for subclasses to customize final results or perform bookkeeping.
   */
  CipherText113a postEncipher(CipherText113a ct) {
    return ct;
  }
}
