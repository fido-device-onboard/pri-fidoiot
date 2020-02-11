// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.sdo.cri.KeyExchangeType.ASYMKEX;
import static org.sdo.cri.KeyExchangeType.ASYMKEX3072;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;

/**
 * RSA asymmetric key exchange (ASYMKEX).
 */
abstract class AsymmetricKeyExchange implements Serializable {

  private static final String CIPHER_ALGO = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
  private final SecureRandom secureRandom;
  private final KeyPair myKeys;

  private byte[] myA = new byte[0];
  private byte[] myB = new byte[0];

  AsymmetricKeyExchange(KeyPair keys, SecureRandom secureRandom) {

    myKeys = keys;
    this.secureRandom = secureRandom;
  }

  byte[] generateShSe() {

    byte[] a = getA();
    byte[] b = getB();
    byte[] shSe = new byte[a.length + b.length];

    ByteBuffer buf = ByteBuffer.wrap(shSe);
    buf.put(b);
    buf.put(a);

    return shSe;
  }

  private byte[] getA() {
    return Arrays.copyOf(myA, myA.length);
  }

  void setA(ByteBuffer a) {
    myA = new byte[a.remaining()];
    a.get(myA);
  }

  private byte[] getB() {
    return Arrays.copyOf(myB, myB.length);
  }

  void setB(ByteBuffer b) {
    myB = new byte[b.remaining()];
    b.get(myB);
  }

  SecureRandom getSecureRandom() {
    return secureRandom;
  }

  // xA is A, untranslated.
  byte[] getXa() {
    return getA();
  }

  void setXa(ByteBuffer xa) {
    setA(xa);
  }

  byte[] getXb() {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGO, BouncyCastleLoader.load());
      cipher.init(Cipher.ENCRYPT_MODE, myKeys.getPublic(), secureRandom);
      return cipher.doFinal(getB());

    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  // xB is B, enciphered with the owner's key.
  void setXb(ByteBuffer xb) {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGO, BouncyCastleLoader.load());
      cipher.init(Cipher.DECRYPT_MODE, myKeys.getPrivate(), secureRandom);
      byte[] byteArray = new byte[xb.remaining()];
      xb.get(byteArray, 0, byteArray.length);
      setB(ByteBuffer.wrap(cipher.doFinal(byteArray)));

    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  int secretSize() {
    switch (kxType()) {

      case ASYMKEX:
        return 256 / 8;

      case ASYMKEX3072:
        return 768 / 8;

      default:
        throw new IllegalArgumentException("kxType = " + kxType());
    }
  }

  KeyExchangeType kxType() {
    RSAPublicKey key = (RSAPublicKey) myKeys.getPublic();
    switch (key.getModulus().bitLength()) {
      case 2048:
        return ASYMKEX;
      case 3072:
        return ASYMKEX3072;
      default:
        throw new IllegalArgumentException();
    }
  }

  static class Device extends AsymmetricKeyExchange implements KeyExchange {

    /**
     * Constructor.
     */
    Device(KeyPair keys, SecureRandom secureRandom) {

      super(keys, secureRandom);

      byte[] b = new byte[super.secretSize()];
      getSecureRandom().nextBytes(b);
      setB(ByteBuffer.wrap(b));
    }

    @Override
    public ByteBuffer generateSharedSecret(ByteBuffer message) {
      setXa(message);
      return ByteBuffer.wrap(generateShSe());
    }

    @Override
    public ByteBuffer getMessage() {
      return ByteBuffer.wrap(getXb());
    }

    @Override
    public KeyExchangeType getType() {
      return kxType();
    }
  }

  static class Owner extends AsymmetricKeyExchange implements KeyExchange {

    /**
     * Constructor.
     */
    Owner(KeyPair keys, SecureRandom secureRandom) {

      super(keys, secureRandom);

      byte[] a = new byte[super.secretSize()];
      getSecureRandom().nextBytes(a);
      setA(ByteBuffer.wrap(a));
    }

    @Override
    public ByteBuffer generateSharedSecret(ByteBuffer message) {
      setXb(message);
      return ByteBuffer.wrap(generateShSe());
    }

    @Override
    public ByteBuffer getMessage() {
      return ByteBuffer.wrap(getXa());
    }

    @Override
    public KeyExchangeType getType() {
      return kxType();
    }
  }
}
