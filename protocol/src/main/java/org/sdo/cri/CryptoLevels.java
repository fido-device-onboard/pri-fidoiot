// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;
import javax.crypto.SecretKey;

/**
 * Utility functions for choosing security services which vary with key type.
 */
abstract class CryptoLevels {

  /**
   * Return the right DigestService for the given key type.
   */
  static CryptoLevel keyTypeToCryptoLevel(final KeyType keyType) {
    switch (keyType) {

      case RSA2048RESTR:
      case ECDSA_P_256:
        return cryptoLevel0();

      case RSA_UR:
      case ECDSA_P_384:
        return cryptoLevel1();

      default:
        throw new IllegalArgumentException("unsupported key type");
    }
  }

  /**
   * Return the right DigestService for the given key exchange type.
   */
  static CryptoLevel keyExchangeTypeToCryptoLevel(KeyExchangeType kxType) {
    switch (kxType) {

      case ASYMKEX:
      case DHKEXid14:
      case ECDH:
        return cryptoLevel0();

      case ASYMKEX3072:
      case DHKEXid15:
      case ECDH384:
        return cryptoLevel1();

      default:
        throw new IllegalArgumentException("unsupported key type");
    }
  }

  private static CryptoLevel cryptoLevel0() {
    return new CryptoLevel() {

      @Override
      public DigestService buildDigestService() {
        return new SimpleDigestService(DigestType.SHA256);
      }

      @Override
      public Function<byte[], SecretKey> getSekDerivationFunction() {
        return in -> {
          try {
            return new Aes128KeyFactory(ByteBuffer.wrap(in)).build();
          } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
          }
        };
      }

      @Override
      public Function<byte[], SecretKey> getSvkDerivationFunction() {
        return in -> {
          try {
            return new Hmac256KeyFactory(ByteBuffer.wrap(in)).build();
          } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
          }
        };
      }
    };
  }

  private static CryptoLevel cryptoLevel1() {
    return new CryptoLevel() {

      @Override
      public DigestService buildDigestService() {
        return new SimpleDigestService(DigestType.SHA384);
      }

      @Override
      public Function<byte[], SecretKey> getSekDerivationFunction() {
        return in -> {
          try {
            return new Aes256KeyFactory(ByteBuffer.wrap(in)).build();
          } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
          }
        };
      }

      @Override
      public Function<byte[], SecretKey> getSvkDerivationFunction() {
        return in -> {
          try {
            return new Hmac384KeyFactory(ByteBuffer.wrap(in)).build();
          } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
          }
        };
      }
    };
  }

  interface CryptoLevel {

    /**
     * Return a new DigestService.
     */
    DigestService buildDigestService();

    /**
     * Return the Session Encryption Key (SEK) derivation function.
     */
    Function<byte[], SecretKey> getSekDerivationFunction();

    /**
     * Return the Session Verification Key (SVK) derivation function.
     */
    Function<byte[], SecretKey> getSvkDerivationFunction();
  }
}
