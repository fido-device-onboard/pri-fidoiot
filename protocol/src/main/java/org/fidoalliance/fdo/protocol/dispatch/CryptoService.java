// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import javax.crypto.SecretKey;

import org.fidoalliance.fdo.protocol.EpidService;
import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.protocol.KeyExchangeResult;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.CipherSuiteType;
import org.fidoalliance.fdo.protocol.message.CoseSign1;
import org.fidoalliance.fdo.protocol.message.EncryptionState;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KexMessage;
import org.fidoalliance.fdo.protocol.message.KexParty;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.fidoalliance.fdo.protocol.message.SigInfo;

public interface CryptoService {
  /**
   * Gets secure Random generator.
   *
   * @return An instance of a secure random generator.
   */
  SecureRandom getSecureRandom();


  /**
   * Gets Secure Random bytes form the internal Random number generator.
   *
   * @param size The number of bytes to get.
   * @return Byte array containing the random bytes.
   */
  byte[] getRandomBytes(int size);

  /**
   * Gets the underlying crypto provider being used.
   *
   * @return The crypto provider.
   */
  Provider getProvider();

  /**
   * Creates a random hmac key.
   * @param hashType The hmacType.
   * @return A random key with a byte length equal to the hash length
   */
  byte[] createHmacKey(HashType hashType) throws IOException;



  /**
   * Creates a key pair of the given key type and size.
   * @param keyType PublicKeyType.
   * @param keySize The keys size type.
   * @return A new key pair.
   * @throws IOException A error occurred.
   */
  KeyPair createKeyPair(PublicKeyType keyType, KeySizeType keySize) throws IOException;

  /**
   * Computes hash of data using SHA.
   *
   * @param hashType The hash algorithm to use.
   * @param data     The data to hash.
   * @return An instance of Hash.
   */
  Hash hash(HashType hashType, byte[] data) throws IOException;

  /**
   * Computes hash of data using HMAC.
   *
   *
   * @param hashType The hash algorithm id to use
   * @param secret   The Secret key.
   * @param data     The data to sign.
   * @return An instance of Hash.
   */
  Hash hash(HashType hashType, byte[] secret, byte[] data) throws IOException;

  /**
   * Encodes the key by its certificate.
   * @param keyType The PublicKeyType.
   * @param encType the PublicKeyEncoding.
   * @param cert The certificate chain the public key.
   * @return An instance of OwnerPublicKey.
   */
  OwnerPublicKey encodeKey(PublicKeyType keyType,
      PublicKeyEncoding encType,
      Certificate[] cert);

  /**
   * Decodes the owner public key.
   * @param ownerPublicKey The spec encoded key.
   * @return The Java public key.
   */
  PublicKey decodeKey(OwnerPublicKey ownerPublicKey) throws IOException;

  /**
   * Returns actual signature.
   *
   * @param sigInfoA initial device based information.
   * @return signature.
   */
  SigInfo getSigInfoB(SigInfo sigInfoA) throws IOException;


  /**
   * Creates a cose-sign1 Signature.
   * @param payload The palyoad to sign.
   * @param signingKey The owners private key.
   * @param ownerPublicKey The owners public key.
   * @return The Cose-Sign1 message.
   */
  CoseSign1 sign(byte[] payload, PrivateKey signingKey, OwnerPublicKey ownerPublicKey)
      throws IOException;

  /**
   * Verifies a cose message.
   * @param message The cose Message to be verified.
   * @param ownerKey The owner key.
   * @return True if the message can be verified by owner key.
   */
  boolean verify(CoseSign1 message,OwnerPublicKey ownerKey) throws IOException;



  /**
   * Verifies a cose message.
   * @param message The cose Message to be verified.
   * @param sigInfo SignInfo
   * @return True if the message can be verified by owner key.
   */
  boolean verify(CoseSign1 message, SigInfo sigInfo) throws IOException;

  /**
   * Gets the Key Exchange message.
   * <p>Creates ExchangeA if called on the server</p>
   * <p>Creates ExchangeB if called on the device</p>
   *
   * @param kexSuiteName The name of the Key Exchange Suite.
   * @param party        The party to the Key Ecxhange (A or B)
   * @param ownerKey     The owner key, required for some asymmetric exchanges
   * @return A KexMessage.
   */
  KexMessage getKeyExchangeMessage(String kexSuiteName, KexParty party,
      OwnerPublicKey ownerKey) throws IOException;

  /**
   * Gets the key exchange shared secret.
   *
   * @param suiteName     The name of the keyexchange.
   * @param message       The key exchange message.
   * @param ownState      The state of the key exchange.
   * @param decryptionKey The decryption key for use in asymmetric exchanges
   * @return The shared secret based on the state and message.
   */
  KeyExchangeResult getSharedSecret(String suiteName, byte[] message, KexMessage ownState,
      Key decryptionKey) throws IOException;

  /**
   * gets the encryption state.
   *
   * @param kxResult    Shared secret and contextRandom.
   * @param cipherType The cipher suite to use.
   * @return A Composite encryption state.
   */
  EncryptionState getEncryptionState(KeyExchangeResult kxResult, CipherSuiteType cipherType)
      throws IOException;

  /**
   * Encrypts a message.
   *
   * @param payload The payload to encrypt.
   * @param state   The saved crypto state.
   * @return The encrypted message.
   */
  byte[] encrypt(byte[] payload, EncryptionState state) throws IOException;

  /**
   * Decrypts a message.
   *
   * @param message The ciphered message.
   * @param state   The crypto state.
   * @return The decrypted message.
   */
  byte[] decrypt(byte[] message, EncryptionState state) throws IOException;

  /**
   * Destroys the private key in the keypair.
   * @param pair The key pair to destroy.
   */
  void destroyKey(KeyPair pair);

  /**
   * Destroys the private key in the keypair.
   * @param privateKey The private key to destroy.
   */
  void destroyKey(PrivateKey privateKey);

  /**
   * Destroys the private key in the keypair.
   * @param key The secret key to destroy.
   */
  void destroyKey(SecretKey key);


}
