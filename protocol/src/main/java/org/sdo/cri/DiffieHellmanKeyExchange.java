// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Objects;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import org.bouncycastle.crypto.agreement.DHStandardGroups;

abstract class DiffieHellmanKeyExchange implements KeyExchange, Serializable {

  private static final String DIFFIE_HELLMAN = "DiffieHellman";
  private KeyPair keys;
  private SecureRandom secureRandom;

  private DiffieHellmanKeyExchange(SecureRandom secureRandom) {
    this.setSecureRandom(secureRandom);
  }

  @Override
  public ByteBuffer generateSharedSecret(ByteBuffer messageBytes) {

    try {
      KeyAgreement keyAgreement =
          KeyAgreement.getInstance(DIFFIE_HELLMAN, BouncyCastleLoader.load());
      keyAgreement.init(Objects.requireNonNull(getKeys()).getPrivate(),
          params(),
          getSecureRandom());

      KeyFactory keyFactory = KeyFactory.getInstance(DIFFIE_HELLMAN, BouncyCastleLoader.load());
      KeySpec keySpec = new DHPublicKeySpec(new BigInteger(1, Buffers.unwrap(messageBytes)),
          params().getP(),
          params().getG());
      Key theirs = keyFactory.generatePublic(keySpec);
      keyAgreement.doPhase(theirs, true);
      byte[] shared = keyAgreement.generateSecret();
      return ByteBuffer.wrap(shared);

    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private KeyPair getKeys() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {

    if (null == keys) {
      KeyPairGenerator keyPairGenerator =
          KeyPairGenerator.getInstance(DIFFIE_HELLMAN, BouncyCastleLoader.load());
      keyPairGenerator.initialize(params(), getSecureRandom());
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      setKeys(keyPair);
    }

    return keys;
  }

  private void setKeys(KeyPair keys) {
    this.keys = keys;
  }

  @Override
  public ByteBuffer getMessage() {

    DHPublicKey key;

    try {
      key = (DHPublicKey) Objects.requireNonNull(getKeys()).getPublic();

    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    return ByteBuffer.wrap(
        BigIntegers.toByteArray(key.getY(), params().getP().bitLength() / 8));
  }

  private SecureRandom getSecureRandom() {
    return secureRandom;
  }

  private void setSecureRandom(SecureRandom secureRandom) {
    this.secureRandom = Objects.requireNonNull(secureRandom);
  }

  protected abstract DHParameterSpec params();

  /**
   * Performs Diffie-Hellman exchange using RFC 3526 MODP group 14.
   */
  static class Group14 extends DiffieHellmanKeyExchange {

    public Group14(SecureRandom secureRandom) {
      super(secureRandom);
    }

    @Override
    public KeyExchangeType getType() {
      return KeyExchangeType.DHKEXid14;
    }

    @Override
    protected DHParameterSpec params() {
      return new DHParameterSpec(
          DHStandardGroups.rfc3526_2048.getP(), DHStandardGroups.rfc3526_2048.getG());
    }
  }

  /**
   * Performs Diffie-Hellman exchange using RFC 3526 MODP group 15.
   */
  static class Group15 extends DiffieHellmanKeyExchange {

    public Group15(SecureRandom secureRandom) {
      super(secureRandom);
    }

    @Override
    public KeyExchangeType getType() {
      return KeyExchangeType.DHKEXid15;
    }

    @Override
    protected DHParameterSpec params() {
      return new DHParameterSpec(
          DHStandardGroups.rfc3526_3072.getP(), DHStandardGroups.rfc3526_3072.getG());
    }
  }
}
