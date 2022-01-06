package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import javax.crypto.SecretKey;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

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
