package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.CertChain;
import org.fidoalliance.fdo.protocol.message.CoseKey;
import org.fidoalliance.fdo.protocol.message.CoseKeyCurveType;
import org.fidoalliance.fdo.protocol.message.CryptoKey;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

public class StandardCryptoService implements CryptoService {

  //java key factory algorithms


  //java certificate chain algorithms
  //public static final String X509_ALG_NAME = "X.509";
  public static final String VALIDATOR_ALG_NAME = "PKIX";

  public static final int GCM_TAG_LENGTH = 128;
  public static final int BIT_LEN_128 = 128;
  public static final int BIT_LEN_256 = 256;
  public static final int BIT_LEN_384 = 384;
  public static final int BIT_LEN_2K = 2 * 1024;
  public static final int BIT_LEN_3K = 3 * 1024;
  public static final int RESTR_EXPONENT = 65537;


  protected static final SecureRandom random = getInitializedRandom();
  private static final Provider BCPROV = getInitializedProvider();


  private static SecureRandom getInitializedRandom() {

    try {
      return SecureRandom.getInstanceStrong();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Strong secure random required", e);
    }
  }

  private static Provider getInitializedProvider() {
    Provider result = new BouncyCastleProvider();
    Security.addProvider(result);
    return result;
  }

  /**
   * Gets secure Random generator.
   *
   * @return An instance of a secure random generator.
   */
  @Override
  public SecureRandom getSecureRandom() {
    return random;
  }


  /**
   * Gets Secure Random bytes form the internal Random number generator.
   *
   * @param size The number of bytes to get.
   * @return Byte array containing the random bytes.
   */
  @Override
  public byte[] getRandomBytes(int size) {
    final byte[] buffer = new byte[size];
    getSecureRandom().nextBytes(buffer);
    return buffer;
  }

  @Override
  public Provider getProvider() {
    return BCPROV;
  }

  @Override
  public byte[] createHmacKey(HashType hashType) throws IOException {
    switch (hashType) {
      case HMAC_SHA256:
        return getRandomBytes(new SHA256Digest().getDigestSize());
      case HMAC_SHA384:
        return getRandomBytes(new SHA384Digest().getDigestSize());
      default:
        throw new IOException(new IllegalArgumentException("not a hmac type"));
    }
  }

  @Override
  public KeyPair createKeyPair(PublicKeyType keyType, KeySizeType keySize) throws IOException {

    switch (keyType) {

      case RSA2048RESTR:
      case RSAPKCS:

        try {
          KeyPairGenerator kg = KeyPairGenerator.getInstance(
              new AlgorithmFinder().getAlgorithm(keyType));

          RSAKeyGenParameterSpec rsaSpec =
              new RSAKeyGenParameterSpec(keySize.toInteger(), RSAKeyGenParameterSpec.F4);

          kg.initialize(rsaSpec, getSecureRandom());

          return kg.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
          throw new IOException(e);
        }

      case SECP384R1:
      case SECP256R1:

        try {
          if (keySize != KeySizeType.SIZE_256) {
            new InvalidAlgorithmParameterException();
          }
          final AlgorithmFinder algorithmFinder = new AlgorithmFinder();
          final CoseKeyCurveType coseKeyCurveType = algorithmFinder.getCoseKeyCurve(keyType);
          final String curveName = algorithmFinder.getAlgorithm(coseKeyCurveType);
          final KeyPairGenerator kg = KeyPairGenerator.getInstance(
              algorithmFinder.getAlgorithm(keyType));
          ECGenParameterSpec ecSpec = new ECGenParameterSpec(curveName);
          kg.initialize(ecSpec, getSecureRandom());
          return kg.generateKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
          throw new IOException(e);
        }

      default:
        throw new IOException(new NoSuchAlgorithmException());
    }

  }


  @Override
  public Hash hash(HashType hashType, byte[] data) throws IOException {
    try {
      final String algName = new AlgorithmFinder().getAlgorithm(hashType);
      final MessageDigest digest = MessageDigest.getInstance(algName, getProvider());

      final Hash hash = new Hash();
      hash.setHashType(hashType);
      hash.setHashValue(digest.digest(data));

      return hash;

    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }


  @Override
  public Hash hash(HashType hashType, byte[] secret, byte[] data) throws IOException {
    SecretKey secretKey = null;
    try {

      Hash hash = new Hash();
      hash.setHashType(hashType);

      String algName = new AlgorithmFinder().getAlgorithm(hashType);
      final Mac mac = Mac.getInstance(algName, getProvider());

      final byte[] macData;
      secretKey = new SecretKeySpec(secret, algName);
      try {
        mac.init(secretKey);
        hash.setHashValue(mac.doFinal(data));
      } finally {
        destroyKey(secretKey);
      }

      return hash;

    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new IOException(e);
    } finally {
      destroyKey(secretKey);
    }
  }

  @Override
  public OwnerPublicKey encodeKey(PublicKeyType keyType, PublicKeyEncoding encType,
      Certificate[] chain) {

    OwnerPublicKey ownerKey = new OwnerPublicKey();
    ownerKey.setType(keyType);
    ownerKey.setEnc(encType);

    switch (encType) {
      case X509: {
        PublicKey publicKey = chain[0].getPublicKey();
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
        ownerKey.setBody(AnyType.fromObject(spec.getEncoded()));
      }
      break;
      case COSEX5CHAIN: {
        List<Certificate> certList = new ArrayList<>(Arrays.asList(chain));
        ownerKey.setBody(AnyType.fromObject(CertChain.fromList(certList)));
      }
      break;
      case COSEKEY: {
        final ECPublicKey ec = (ECPublicKey) chain[0].getPublicKey();
        final byte[] x = ec.getW().getAffineX().toByteArray();
        final byte[] y = ec.getW().getAffineY().toByteArray();

        final CoseKey coseKey = new CoseKey();
        coseKey.setX(x);
        coseKey.setY(y);
        coseKey.setCurve(new AlgorithmFinder().getCoseKeyCurve(keyType));
        ownerKey.setBody(AnyType.fromObject(coseKey));

      }
      break;
      case CRYPTO: {

        final RSAPublicKey key = (RSAPublicKey) chain[0].getPublicKey();
        final byte[] mod = key.getModulus().toByteArray();
        final byte[] exp = key.getPublicExponent().toByteArray();
        final CryptoKey cryptoKey = new CryptoKey();
        cryptoKey.setModulus(mod);
        cryptoKey.setExponent(exp);
        ownerKey.setBody(AnyType.fromObject(cryptoKey));

      }
      break;
      default:
        throw new NoSuchElementException();
    }

    return ownerKey;
  }

  @Override
  public PublicKey decodeKey(OwnerPublicKey ownerPublicKey) throws IOException {

    try {
      switch (ownerPublicKey.getEnc()) {
        case CRYPTO: {
          final CryptoKey key = ownerPublicKey.getBody().covertValue(CryptoKey.class);

          final BigInteger mod = new BigInteger(1, key.getModulus());
          final BigInteger exp = new BigInteger(1, key.getExponent());

          final RSAPublicKeySpec rsaPkSpec = new RSAPublicKeySpec(mod, exp);
          final KeyFactory factory = KeyFactory.getInstance(
              new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()));
          return factory.generatePublic(rsaPkSpec);
        }
        case X509: {
          final byte[] x509body = ownerPublicKey.getBody().covertValue(byte[].class);
          final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509body);
          final KeyFactory factory = KeyFactory.getInstance(
              new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()));

          return factory.generatePublic(keySpec);
        }
        case COSEX5CHAIN: {
          final CertChain chain = ownerPublicKey.getBody().covertValue(CertChain.class);
          return chain.getChain().get(0).getPublicKey();
        }
        case COSEKEY: {

          AlgorithmFinder algFinder = new AlgorithmFinder();
          AlgorithmParameters params = AlgorithmParameters.getInstance(
              algFinder.getAlgorithm(ownerPublicKey.getType()));

          CoseKey coseKey = ownerPublicKey.getBody().covertValue(CoseKey.class);

          params.init(new ECGenParameterSpec(algFinder.getAlgorithm(coseKey.getCrv())));

          ECParameterSpec ecParameterSpec = params.getParameterSpec(ECParameterSpec.class);

          ECPoint ecPoint = new ECPoint(new BigInteger(1, coseKey.getX()),
              new BigInteger(1, coseKey.getY()));

          final KeyFactory factory = KeyFactory.getInstance(
              new AlgorithmFinder().getAlgorithm(ownerPublicKey.getType()));

          return factory.generatePublic(
              new ECPublicKeySpec(ecPoint, ecParameterSpec));
        }
        default:
          throw new IOException(new IllegalArgumentException("key not valid"));
      }
    } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void destroyKey(KeyPair pair) {
    if (pair != null) {
      destroyKey(pair.getPrivate());
    }
  }

  @Override
  public void destroyKey(PrivateKey privateKey) {
    if (privateKey != null && !privateKey.isDestroyed()) {
      try {
        privateKey.destroy();
      } catch (DestroyFailedException e) {
        //crypto lib does not support destroy
      }
    }
  }

  @Override
  public void destroyKey(SecretKey key) {
    if (key != null && !key.isDestroyed()) {
      try {
        key.destroy();
      } catch (DestroyFailedException e) {
        //crypto lib does not support destroy
      }
    }
  }

  /**
   * Encodes a public key.
   *
   *  pubKey The public key encoded as X509.
   * The encoded ownerKey.
   *

   public OwnerPublicKey encodeKey(Certificate cert, PublicKeyEncoding enc) {

   OwnerPublicKey ownerKey = new OwnerPublicKey();
   if (pubKey instanceof ECPublicKey) {
   final ECPublicKey ec = (ECPublicKey) pubKey;
   final byte[] x = ec.getW().getAffineX().toByteArray();
   final byte[] y = ec.getW().getAffineY().toByteArray();
   final int bitLength = getKeySize();

   if (bitLength == BIT_LEN_256) {
   ownerKey.setType(PublicKeyType.SECP256R1);
   } else if (bitLength == BIT_LEN_384) {
   ownerKey.setType(PublicKeyType.SECP384R1);
   }
   } else if (pubKey instanceof RSAPublicKey) {
   final RSAPublicKey rsa = (RSAPublicKey) pubKey;
   int bitLength = rsa.getModulus().bitLength();
   int exponent = rsa.getPublicExponent().intValue();
   if (bitLength == BIT_LEN_256 && exponent == RESTR_EXPONENT) {
   ownerKey.setType(PublicKeyType.RSA2048RESTR);
   } else {
   ownerKey.setType(PublicKeyType.RSAPKCS);
   }
   }*/
    /*
    However, the ownership voucher signing and key encoding must be consistent across all entries in the ownership voucher. This is required to ensure that the Device is able to process each entry.
    The restricted RSA public key, RSA2048RESTR is an RSA key with 2048 bits of base and an exponent equal to 65537
    Public keys in Ownership Voucher (all must have same size, type and hash)	RSA2048RESTR (RSA with 2048-bit key, restricted exponent)
    RSAPKCS with 2048-bit key
    RSAPKCS with 3072-bit key
    RSAPSS with 3072-bit key
    ECDSA secp256r1
    ECDSA secp384r1
    RSAPSS with 2048-bit key probably goes away*/
/*
    X509EncodedKeySpec x509 = new X509EncodedKeySpec(pubKey.getEncoded());
    ownerKey.setEnc(PublicKeyEncoding.X509);
    ownerKey.setBody(AnyType.fromObject(x509.getEncoded()));
    return ownerKey;
  }*/


}
