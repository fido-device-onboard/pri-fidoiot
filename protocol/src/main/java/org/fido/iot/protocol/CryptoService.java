// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;

import org.fido.iot.protocol.epid.EpidMaterialService;
import org.fido.iot.protocol.epid.EpidSignatureVerifier;
import org.fido.iot.protocol.ondie.OnDieService;

/**
 * Cryptography Service.
 *
 * <p>Performs cryptographic operations a formats cryptographic data</p>
 */
public class CryptoService {

  static final BouncyCastleProvider BCPROV = new BouncyCastleProvider();
  private SecureRandom secureRandom;
  private boolean epidTestMode = false;

  /**
   * Constructs a CryptoService from a list of algorithm names.
   *
   * @param algNames Random number generation algorithm names.
   */
  public CryptoService(String[] algNames) {

    SecureRandom rnd = null;
    if (algNames != null && algNames.length > 0) {
      for (String alg : algNames) {
        try {
          rnd = SecureRandom.getInstance(alg);
          break;
        } catch (NoSuchAlgorithmException e) {
          //suppress priority list
        }
      }
    }
    // no algorithm found in priority list
    if (rnd == null) {
      try {
        rnd = SecureRandom.getInstanceStrong();
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }
    secureRandom = rnd;
  }

  /**
   * Constructs a CryptoService using default secure random number generator.
   */
  public CryptoService() {
    try {
      secureRandom = SecureRandom.getInstanceStrong();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the epid test mode.
   *
   */
  public void setEpidTestMode() {
    this.epidTestMode = true;
  }

  /**
   * Gets the secure random number generator.
   *
   * @return The Random number generator being used by the service.
   */
  public SecureRandom getSecureRandom() {
    return secureRandom;
  }

  /**
   * Gets the security provider key factory algorithm from the fido spec key type.
   *
   * @param algId The spec key type.
   * @return The key factor algorithm.
   * @throws NoSuchAlgorithmException If the algorithm is not supported.
   */
  protected String getKeyFactoryAlgorithm(int algId) throws NoSuchAlgorithmException {
    switch (algId) {
      case Const.PK_SECP256R1:
      case Const.PK_SECP384R1:
      case Const.PK_ONDIE_ECDSA_384:
        return Const.EC_ALG_NAME;
      case Const.PK_RSA2048RESTR:
      case Const.PK_RSA:
        return Const.RSA_ALG_NAME;
      default:
        throw new NoSuchAlgorithmException();
    }
  }

  /**
   * get the MAC key given a secret.
   *
   * @param secret The secret.
   * @param algName The HMAC Algorithm.
   * @return The key based on the secret and HMAC algorithm.
   */
  protected Key getHmacKey(byte[] secret, String algName) {
    return new SecretKeySpec(secret, algName);
  }

  protected KeyFactory getKeyFactoryInstance(String algName) throws NoSuchAlgorithmException {
    return KeyFactory.getInstance(algName);
  }

  protected Mac getMacInstance(String algName) throws NoSuchAlgorithmException {
    return Mac.getInstance(algName);
  }

  protected MessageDigest getDigestInstance(String algName) throws NoSuchAlgorithmException {
    return MessageDigest.getInstance(algName);
  }

  protected Signature getSignatureInstance(String algName) throws NoSuchAlgorithmException {
    return Signature.getInstance(algName);
  }

  protected KeyAgreement getKeyAgreementInstance(String algName) throws NoSuchAlgorithmException {
    return KeyAgreement.getInstance(algName);
  }

  protected Cipher getCipherInstance(String algName)
      throws NoSuchPaddingException, NoSuchAlgorithmException {
    switch (algName) {
      case Const.AES128_CTR_HMAC256_ALG_NAME:
      case Const.AES256_CTR_HMAC384_ALG_NAME:
        return Cipher.getInstance("AES/CTR/NoPadding");
      default:
        throw new CryptoServiceException(new NoSuchAlgorithmException());
    }
  }

  protected String getHashAlgorithm(int algId) throws NoSuchAlgorithmException {
    switch (algId) {
      case Const.SHA_256:
        return Const.SHA_256_ALG_NAME;
      case Const.SHA_384:
        return Const.SHA_384_ALG_NAME;
      case Const.SHA_512:
        return Const.SHA_512_ALG_NAME;
      case Const.HMAC_SHA_256:
      case Const.HMAC_SHA_256_KX:
        return Const.HMAC_256_ALG_NAME;
      case Const.HMAC_SHA_384:
      case Const.HMAC_SHA_384_KX:
        return Const.HMAC_384_ALG_NAME;
      case Const.HMAC_SHA_512_KX:
        return Const.HMAC_512_ALG_NAME;
      default:
        throw new NoSuchAlgorithmException();
    }
  }

  /**
   * Gets the Signature algorithm from COSE algorithm id.
   *
   * @param coseAlg The COSE algorithm Id.
   * @return The Signature algorithm represented by the COSE algorithm id.
   * @throws NoSuchAlgorithmException If the cose Algorithm is not supported.
   */
  public String getSignatureAlgorithm(int coseAlg) throws NoSuchAlgorithmException {
    switch (coseAlg) {
      case Const.COSE_ES256:
        return Const.ECDSA_256_ALG_NAME;
      case Const.COSE_ES384:
      case Const.COSE_ES384_ONDIE:
        return Const.ECDSA_384_ALG_NAME;
      case Const.COSE_RS384:
        return Const.RSA_384_ALG_NAME;
      case Const.COSE_RS256:
        return Const.RSA_256_ALG_NAME;
      default:
        throw new NoSuchAlgorithmException();
    }
  }

  /**
   * Gets the COSE algorithm id from a key.
   *
   * @param key A public or private key.
   * @return The COSE algorithm id.
   */
  public int getCoseAlgorithm(Key key) {
    if (key instanceof ECKey) {
      int bitLength = ((ECKey) key).getParams().getCurve().getField().getFieldSize();
      if (bitLength == Const.BIT_LEN_256) {
        return Const.COSE_ES256;
      } else if (bitLength == Const.BIT_LEN_384) {
        return Const.COSE_ES384;
      }
    } else if (key instanceof RSAKey) {
      int bitLength = ((RSAKey) key).getModulus().bitLength();
      if (Const.BIT_LEN_2K == bitLength) {
        return Const.COSE_RS256;
      } else if (Const.BIT_LEN_3K == bitLength) {
        return Const.COSE_RS384;
      }
    }
    throw new RuntimeException(new NoSuchAlgorithmException());
  }

  /**
   * Get the public key type id.
   *
   * @param key A public or private key.
   * @return The spec public key type.
   */
  public int getPublicKeyType(Key key) {
    if (key instanceof ECKey) {
      int bitLength = ((ECKey) key).getParams().getCurve().getField().getFieldSize();
      if (bitLength == Const.BIT_LEN_256) {
        return Const.PK_SECP256R1;
      }
      if (bitLength == Const.BIT_LEN_384) {
        return Const.PK_SECP384R1;
      }
    } else if (key instanceof RSAKey) {
      return Const.PK_RSA2048RESTR;
    }
    throw new RuntimeException(new NoSuchAlgorithmException());
  }

  /**
   * Gets SignInfo associated with the key.
   *
   * @param key A Public key
   * @return The signInfo
   */
  public Composite getSignInfo(Key key) {
    if (key instanceof ECKey) {
      int bitLength = ((ECKey) key).getParams().getCurve().getField().getFieldSize();
      if (bitLength == Const.BIT_LEN_256) {
        return Composite.newArray()
            .set(Const.SG_TYPE, Const.PK_SECP256R1)
            .set(Const.SG_INFO, Const.EMPTY_BYTE);
      }
      if (bitLength == Const.BIT_LEN_384) {
        return Composite.newArray()
            .set(Const.SG_TYPE, Const.PK_SECP384R1)
            .set(Const.SG_INFO, Const.EMPTY_BYTE);
      }
    } else if (key instanceof RSAKey) {
      int bitLength = ((RSAKey)key).getModulus().bitLength();
      if (Const.BIT_LEN_2K == bitLength) {
        return Composite.newArray()
            .set(Const.SG_TYPE, Const.PK_RSA2048RESTR)
            .set(Const.SG_INFO, Const.EMPTY_BYTE);
      } else if (Const.BIT_LEN_3K == bitLength) {
        return Composite.newArray()
            .set(Const.SG_TYPE, Const.PK_RSA)
            .set(Const.SG_INFO, Const.EMPTY_BYTE);
      }
    }
    throw new RuntimeException(new NoSuchAlgorithmException());
  }

  /**
   * Verify bytes are equal.
   *
   * @param nonce1 the first byte array to compare.
   * @param nonce2 the second byte array to compare.
   * @throws InvalidMessageException If nonce1 and nonce2 are not equal.
   */
  public void verifyBytes(byte[] nonce1, byte[] nonce2) {
    if (ByteBuffer.wrap(nonce1).compareTo(ByteBuffer.wrap(nonce2)) != 0) {
      throw new InvalidMessageException();
    }
  }

  /**
   * Verifies the hash of a payload.
   *
   * @param hashValue A composite representing a hash.
   * @param payload The payload to verify.
   */
  public void verifyHash(Composite hashValue, byte[] payload) {

    Composite hashResult = hash(hashValue.getAsNumber(Const.HASH_TYPE).intValue(), payload);
    ByteBuffer hash1 = hashValue.getAsByteBuffer(Const.HASH);
    ByteBuffer hash2 = hashResult.getAsByteBuffer(Const.HASH);

    if (hash1.compareTo(hash2) != 0) {
      throw new InvalidMessageException();
    }
  }

  /**
   * Determines if keys are equal.
   *
   * @param key1 The first key.
   * @param key2 The second key.
   * @return True if key1 equals key2, otherwise false.
   */
  public int compare(PublicKey key1, PublicKey key2) {

    if (key1 instanceof ECPublicKey && key1 instanceof ECPublicKey) {
      ECPublicKey ecc1 = (ECPublicKey) key1;
      ECPublicKey ecc2 = (ECPublicKey) key2;

      if (ecc1.getW().getAffineX().equals(ecc2.getW().getAffineX())
          && ecc1.getW().getAffineY().equals(ecc2.getW().getAffineY())) {
        return 0;
      }

    } else if (key1 instanceof RSAPublicKey && key2 instanceof RSAPublicKey) {
      RSAPublicKey rsa1 = (RSAPublicKey) key1;
      RSAPublicKey rsa2 = (RSAPublicKey) key2;

      if (rsa1.getModulus().equals(rsa2.getModulus())
          && rsa1.getPublicExponent().equals(rsa2.getPublicExponent())) {

        return 0;
      }
    }
    return -1;
  }

  /**
   * Gets the compatible encoding type for the key.
   *
   * @param key A public key.
   * @return The supported encoding type for the key.
   */
  public int getCompatibleEncoding(PublicKey key) {
    if (key instanceof ECPublicKey) {
      return Const.PK_ENC_COSEEC;
    } else if (key instanceof RSAPublicKey) {
      return Const.PK_ENC_X509;
    }
    throw new RuntimeException(new InvalidKeyException());
  }

  /**
   * Get the Hash type id compatible with a key.
   *
   * @param key A public key.
   * @return The hash id compatible with the key.
   */
  public int getCompatibleHashType(PublicKey key) {

    if (key instanceof ECPublicKey) {
      int bitLength = ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize();
      if (bitLength == Const.BIT_LEN_256) {
        return Const.SHA_256;
      }
      if (bitLength == Const.BIT_LEN_384) {
        return Const.SHA_384;
      }
    } else if (key instanceof RSAPublicKey) {
      int bitLength = ((RSAPublicKey) key).getModulus().bitLength();
      if (2 * 1024 == bitLength) {
        return Const.SHA_256;
      } else {
        return Const.SHA_384;
      }
    }
    throw new RuntimeException(new InvalidKeyException());
  }

  /**
   * Gets the Hash algorithm id.
   *
   * @param key A public key.
   * @return The supporting HMAC algorithm id for the given key.
   */
  public int getCompatibleHmacType(PublicKey key) {

    if (key instanceof ECPublicKey) {
      int bitLength = ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize();
      if (bitLength == Const.BIT_LEN_256) {
        return Const.HMAC_SHA_256;
      }
      if (bitLength == Const.BIT_LEN_384) {
        return Const.HMAC_SHA_384;
      }
    } else if (key instanceof RSAPublicKey) {
      int bitLength = ((RSAPublicKey) key).getModulus().bitLength();
      if (2 * 1024 == bitLength) {
        return Const.HMAC_SHA_256;
      } else {
        return Const.HMAC_SHA_384;
      }
    }
    throw new CryptoServiceException(new NoSuchAlgorithmException());
  }

  /**
   * Returns actual signature.
   *
   * @param sigInfoA initial device based information
   * @return signature
   */
  public Composite getSigInfoB(Composite sigInfoA) {
    if (null != sigInfoA && sigInfoA.size() > 0
            && Arrays.asList(Const.SG_EPIDv10, Const.SG_EPIDv11)
            .contains(sigInfoA.getAsNumber(Const.FIRST_KEY).intValue())) {
      EpidMaterialService epidMaterialService = new EpidMaterialService();
      try {
        return epidMaterialService.getSigInfo(sigInfoA);
      } catch (IOException ioException) {
        throw new RuntimeException(ioException);
      }
    }
    return sigInfoA;
  }

  protected byte[] adjustBigBuffer(byte[] buffer, int byteLength) {
    final ByteBuffer result = ByteBuffer.allocate(byteLength);
    int skip = 0;
    //skip leading zero that BigInteger may add
    while ((buffer.length - skip) > byteLength) {
      skip++;
    }
    int pad = 0;
    // left pad with zero if not correct size
    while (buffer.length + pad < byteLength) {
      pad++;
      result.put((byte) 0);
    }
    result.put(buffer, skip, buffer.length - skip);
    result.flip();
    return Composite.unwrap(result);
  }

  /**
   * Encodes the public key.
   *
   * @param publicKey A public key.
   * @param encType A compatible encoding type.
   * @return The encode public key as a Composite.
   */
  public Composite encode(PublicKey publicKey, int encType) {
    final Composite pm = Composite.newArray();
    pm.set(Const.PK_ENC, encType);
    switch (encType) {

      case Const.PK_ENC_COSEEC: {

        final ECPublicKey ec = (ECPublicKey) publicKey;
        final byte[] x = ec.getW().getAffineX().toByteArray();
        final byte[] y = ec.getW().getAffineY().toByteArray();
        final int bitLength = ec.getParams().getCurve().getField().getFieldSize();
        final int byteLength = bitLength / Byte.SIZE;

        final ByteBuffer body = ByteBuffer.allocate(byteLength * 2);
        body.put(adjustBigBuffer(x, byteLength));
        body.put(adjustBigBuffer(y, byteLength));
        body.flip();

        if (bitLength == Const.BIT_LEN_256) {
          pm.set(Const.PK_TYPE, Const.PK_SECP256R1);
        } else if (bitLength == Const.BIT_LEN_384) {
          pm.set(Const.PK_TYPE, Const.PK_SECP384R1);
        } else {
          throw new CryptoServiceException(new InvalidKeyException());
        }
        pm.set(Const.PK_BODY, body);

      }
      break;
      case Const.PK_ENC_CRYPTO: {
        RSAPublicKey key = (RSAPublicKey) publicKey;
        final ByteBuffer mod = ByteBuffer.wrap(key.getModulus().toByteArray());
        final ByteBuffer exp = ByteBuffer.wrap(key.getPublicExponent().toByteArray());

        Composite pkbody = Composite.newArray()
                .set(Const.FIRST_KEY, mod)
                .set(Const.SECOND_KEY, exp);

        pm.set(Const.PK_BODY, pkbody);
        pm.set(Const.PK_TYPE, getPublicKeyType(publicKey));
      }
      break;
      case Const.PK_ENC_X509: {
        X509EncodedKeySpec x509 = new X509EncodedKeySpec(publicKey.getEncoded());
        pm.set(Const.PK_ENC, Const.PK_ENC_X509);
        pm.set(Const.PK_BODY, x509.getEncoded());
        pm.set(Const.PK_TYPE, getPublicKeyType(publicKey));
      }
        break;
      default:
        throw new CryptoServiceException(new NoSuchAlgorithmException());
    }
    return pm;
  }

  /**
   * Gets the public key represented from the Compiste.
   *
   * @param pub An Composite representing a public Key.
   * @return The Public key.
   */
  public PublicKey decode(Composite pub) {

    final int keyType = pub.getAsNumber(Const.PK_TYPE).intValue();
    final int keyEnc = pub.getAsNumber(Const.PK_ENC).intValue();

    KeyFactory factory;
    try {
      switch (keyEnc) {
        case Const.PK_ENC_CRYPTO:
          Composite cryptoBody = pub.getAsComposite(Const.PK_BODY);
          BigInteger mod = new BigInteger(1, cryptoBody.getAsBytes(Const.FIRST_KEY));
          BigInteger exp = new BigInteger(1, cryptoBody.getAsBytes(Const.SECOND_KEY));

          RSAPublicKeySpec rsaPkSpec = new RSAPublicKeySpec(mod, exp);
          factory = getKeyFactoryInstance(getKeyFactoryAlgorithm(keyType));
          return factory.generatePublic(rsaPkSpec);

        case Const.PK_ENC_X509:
          final byte[] x509body = pub.getAsBytes(Const.PK_BODY);
          final X509EncodedKeySpec rsaSpec = new X509EncodedKeySpec(x509body);
          factory = getKeyFactoryInstance(getKeyFactoryAlgorithm(keyType));

          return factory.generatePublic(rsaSpec);

        case Const.PK_ENC_COSEEC:
          final byte[] coseEecBody = pub.getAsBytes(Const.PK_BODY);
          final ByteBuffer buf509;
          if (keyType == Const.PK_SECP256R1) {
            buf509 = ByteBuffer.allocate(Const.X509_EC256_HEADER.length + coseEecBody.length);
            buf509.put(Const.X509_EC256_HEADER);
          } else if (keyType == Const.PK_SECP384R1) {
            buf509 = ByteBuffer.allocate(Const.X509_EC384_HEADER.length + coseEecBody.length);
            buf509.put(Const.X509_EC384_HEADER);
          } else {
            throw new NoSuchAlgorithmException();
          }
          buf509.put(coseEecBody);
          buf509.flip();

          factory = getKeyFactoryInstance(getKeyFactoryAlgorithm(keyType));
          final X509EncodedKeySpec escSpec = new X509EncodedKeySpec(Composite.unwrap(buf509));
          return factory.generatePublic(escSpec);

        default:
          throw new CryptoServiceException(new InvalidParameterException());
      }
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * Gets Secure Random bytes form the internal Random number generator.
   *
   * @param size The number of bytes to get.
   * @return Byte array containing the random bytes.
   */
  public byte[] getRandomBytes(int size) {
    final byte[] buffer = new byte[size];
    getSecureRandom().nextBytes(buffer);
    return buffer;
  }

  /**
   * Verifies MAROE Prefix.
   *
   * @param cose The COSE message
   */
  public void verifyMaroePrefix(Composite cose) {
    Composite uph = cose.getAsComposite(Const.COSE_SIGN1_UNPROTECTED);
    String maroePrefix = Composite.toString(uph.getAsBytes(Const.EAT_MAROE_PREFIX));
    boolean isMaroePrefixValid = false;
    for (final String maroePrefixItem : Const.MAROE_PREFIX_LIST) {
      if (maroePrefix.equals(maroePrefixItem)) {
        isMaroePrefixValid = true;
        break;
      }
    }
    if (!isMaroePrefixValid) {
      throw new RuntimeException("Invalid MAROE Prefix");
    }
  }

  /**
   * Verifies a COSE signature. Handles generic ECCDSA EPID and OnDie ECDSA signatures.
   *
   * @param verificationKey The verification key to use.
   * @param cose            The COSE message.
   * @param sigInfoA        The sigInfo object representing eA
   * @param onDieService    The onDie service.
   * @param onDieCertPath   The onDie cert path.
   * @return True if the signature matches otherwise false.
   */
  public boolean verify(PublicKey verificationKey,
                        Composite cose,
                        Composite sigInfoA,
                        OnDieService onDieService,
                        Composite onDieCertPath) {
    if (null != sigInfoA && sigInfoA.size() > 0 && Arrays.asList(Const.SG_EPIDv10, Const.SG_EPIDv11)
        .contains(sigInfoA.getAsNumber(Const.FIRST_KEY).intValue())) {
      // EPID verification
      verifyMaroePrefix(cose);
      EpidSignatureVerifier.Result verificationResult =
              EpidSignatureVerifier.verify(cose, sigInfoA);
      if (verificationResult == EpidSignatureVerifier.Result.VERIFIED) {
        return true;
      } else if (this.epidTestMode) {
        // in test mode ignore the validation of EPID signatures but
        // still perform the verification to check for non-signature issues
        if (verificationResult == EpidSignatureVerifier.Result.UNKNOWN_ERROR
                || verificationResult == EpidSignatureVerifier.Result.MALFORMED_REQUEST) {
          return false;
        }
        return true;
      }
      return false;
    }

    final Composite header1;
    final Object rawHeader = cose.get(Const.COSE_SIGN1_PROTECTED);
    if (rawHeader instanceof byte[]) {
      final byte[] protectedHeader = cose.getAsBytes(Const.COSE_SIGN1_PROTECTED);
      header1 = Composite.fromObject(protectedHeader);
    } else {
      throw new UnsupportedOperationException("Illegal protected header encoding");
    }
    final int algId = header1.getAsNumber(Const.COSE_ALG).intValue();
    final ByteBuffer payload = cose.getAsByteBuffer(Const.COSE_SIGN1_PAYLOAD);
    final byte[] sig = cose.getAsBytes(Const.COSE_SIGN1_SIGNATURE);

    try {
      // OnDie ECDSA signature verification
      if (algId == Const.COSE_ES384_ONDIE) {
        if (onDieService == null) {
          throw new SignatureException("Internal error: onDieService not initialized.");
        }
        try {
          // if cert path is given then check revocations
          // else just check the signature
          if (onDieCertPath != null) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            List<Certificate> certPath = new ArrayList<>();
            for (int i = 0; i < onDieCertPath.size(); i++) {
              certPath.add(
                      cf.generateCertificate(
                              new ByteArrayInputStream((byte[]) onDieCertPath.get(i))));
            }
            return onDieService.validateSignature(certPath, payload.array(), sig);
          } else {
            return onDieService.validateSignature(verificationKey, payload.array(), sig);
          }
        } catch (CertificateException ex) {
          return false;
        }
      }

      // ECDSA verification
      final String algName = getSignatureAlgorithm(algId);
      final Signature signer = getSignatureInstance(algName);

      signer.initVerify(verificationKey);
      signer.update(payload);
      return signer.verify(sig);

    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * Verifies a certificate chain.
   *
   * @param chain A Composite array of certificate
   */
  public void verify(Composite chain) {

    final Set<TrustAnchor> anchors = new HashSet<>();

    try {
      final CertPath cp = getCertPath(chain);

      for (int i = 1; i < chain.size(); i++) {
        X509Certificate anchorCert = (X509Certificate) cp.getCertificates().get(i);
        anchors.add(new TrustAnchor(anchorCert, null));
      }

      final CertPathValidator validator =
            CertPathValidator.getInstance(getValidatorAlgorithm());

      final CertPathParameters params = getCertPathParameters(anchors);

      validator.validate(cp, params);
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
        | CertificateException | CertPathValidatorException e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * Verifies certificate algorithm.
   *
   * @param algorithmName Public key algorithm
   */
  public void verifyAlgorithm(String algorithmName) throws InvalidOwnershipVoucherException {
    if (!(algorithmName.equals(Const.EC_ALG_NAME))) {
      throw new InvalidOwnershipVoucherException(
          "Wrong public key algorithm inside the device certificate - supported: ECDSA, received: "
              + algorithmName);
    }
  }

  /**
   * Verifies certificate public key curve.
   *
   * @param ecdsaCurveName ECDSA curve name
   */
  private void verifyCurveName(String ecdsaCurveName) throws InvalidOwnershipVoucherException {
    if (!(ecdsaCurveName.contains(Const.SECP256R1_CURVE_NAME)
        || ecdsaCurveName.contains(Const.PRIME256V1_CURVE_NAME)
        || ecdsaCurveName.contains(Const.SECP384R1_CURVE_NAME))) {
      throw new InvalidOwnershipVoucherException("Mismatch of ECDSA curve type.");
    }
  }

  /**
   * Verifies certificate public key size.
   *
   * @param pubKeySize Public key size
   */
  private void verifyKeySize(int pubKeySize) throws InvalidOwnershipVoucherException {
    if (pubKeySize != Const.BIT_LEN_256 && pubKeySize != Const.BIT_LEN_384) {
      throw new InvalidOwnershipVoucherException(
          "Wrong public key size. Received public key size: {}." + pubKeySize);
    }
  }

  /**
   * Verifies leaf certificate public key in ownership voucher.
   *
   * @param cert leaf certificate from ownership voucher
   */
  private void verifyLeafPubKeyData(X509Certificate cert) throws InvalidOwnershipVoucherException {
    String publicKeyAlgorithm = cert.getPublicKey().getAlgorithm();
    verifyAlgorithm(publicKeyAlgorithm);

    String ecdsaCurveName;
    ECParameterSpec p = ((ECPublicKey) cert.getPublicKey()).getParams();
    if (p instanceof ECNamedCurveSpec) {
      ECNamedCurveSpec ncs = (ECNamedCurveSpec)p;
      ecdsaCurveName = ncs.getName();
    } else {
      ecdsaCurveName = p.toString();
    }

    verifyCurveName(ecdsaCurveName);

    int pubKeySize =
        ((ECPublicKey) cert.getPublicKey()).getParams().getCurve().getField().getFieldSize();
    verifyKeySize(pubKeySize);
  }

  /**
   * Verifies leaf certificate in ownership voucher.
   *
   * @param cert leaf certificate from ownership voucher
   */
  private void verifyLeafCertPrivileges(X509Certificate cert)
      throws InvalidOwnershipVoucherException {
    if (cert.getKeyUsage() != null) {
      if (!(cert.getKeyUsage()[0])) {
        throw new InvalidOwnershipVoucherException(
            "Digital signature is not allowed for the device certificate");
      }
    }
  }

  /**
   * Verifies certificate chain.
   *
   * @param certChain ownership voucher device certificate chain
   */
  public void verifyCertChain(Composite certChain) {
    X509Certificate leafCertificate = null;
    try {
      final CertPath cp = getCertPath(certChain);
      leafCertificate = (X509Certificate) cp.getCertificates().get(0);
    } catch (CertificateException e) {
      throw new CryptoServiceException(e);
    }

    verifyLeafPubKeyData(leafCertificate);
    verifyLeafCertPrivileges(leafCertificate);
  }

  /**
   * Verifies ownership voucher.
   *
   * @param voucher ownership voucher
   */
  public void verifyVoucher(Composite voucher) {

    verifyHash(
            voucher.getAsComposite(Const.OV_HEADER).getAsComposite(Const.OVH_CERT_CHAIN_HASH),
            voucher.getAsComposite(Const.OV_DEV_CERT_CHAIN).toBytes());
    verifyCertChain(voucher.getAsComposite(Const.OV_DEV_CERT_CHAIN));
    verify(voucher.getAsComposite(Const.OV_DEV_CERT_CHAIN));
  }

  /**
   * Produces a COSE Signature.
   *
   * @param signingKey The signing key.
   * @param payload    The payload to sign.
   * @param coseSignatureAlg The COSE algorithm that determines the signature algorithm.
   * @return The resulting COSE Signature.
   */
  public Composite sign(PrivateKey signingKey, byte[] payload, int coseSignatureAlg) {

    final byte[] protectedHeader = Composite.newMap()
        .set(Const.COSE_ALG, coseSignatureAlg)
        .toBytes();

    final Composite cos = Composite.newArray()
        .set(Const.COSE_SIGN1_PROTECTED, protectedHeader)
        .set(Const.COSE_SIGN1_UNPROTECTED, Composite.newMap())
        .set(Const.COSE_SIGN1_PAYLOAD, payload);

    try {
      final String algName = getSignatureAlgorithm(coseSignatureAlg);

      final Signature signer = getSignatureInstance(algName);
      signer.initSign(signingKey);
      signer.update(payload);
      final byte[] sig = signer.sign();
      cos.set(Const.COSE_SIGN1_SIGNATURE, sig);

      return cos;

    } catch (Exception e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * Computes hash of data using HMAC.
   *
   * <p>The Hash type must be one of the following.
   * <p>HMAC_SHA_256</p>
   * <p>HMAC_SHA_384</p>
   * <p>HMAC_SHA_256_KX</p>
   * <p>HMAC_SHA_512_KX</p>
   * <p>HMAC_SHA_384_KX</p>
   *
   * @param hashType The hash algorithm id to use
   * @param secret   Secret key.
   * @param data     The data to sign.
   * @return Hash composite with HASH_TYPE and HASH keys.
   */
  public Composite hash(int hashType, byte[] secret, byte[] data) {

    try {
      final String algName = getHashAlgorithm(hashType);
      final Mac mac = getMacInstance(algName);

      final Key secretKey = getHmacKey(secret, algName);
      mac.init(secretKey);
      final byte[] macData = mac.doFinal(data);

      return Composite.newArray()
          .set(Const.HASH_TYPE, hashType)
          .set(Const.HASH, macData);

    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * Computes hash of data using SHA.
   * <p>The Composite must contain a HAST_TYPE key set to one of the following:</p>
   * <p>SHA_256</p>
   * <p>SHA_384</p>
   * <p>SHA_512</p>
   *
   * @param hashType The hash algorithm to use.
   * @param data     The data to sign.
   * @return Hash composite with HASH_TYPE and HASH keys.
   */
  public Composite hash(int hashType, byte[] data) {
    try {
      final String algName = getHashAlgorithm(hashType);
      final MessageDigest digest = getDigestInstance(algName);

      final byte[] result = digest.digest(data);

      return Composite.newArray()
          .set(Const.HASH_TYPE, hashType)
          .set(Const.HASH, result);

    } catch (NoSuchAlgorithmException e) {
      throw new CryptoServiceException(e);
    }
  }

  protected String getValidatorAlgorithm() {
    return Const.VALIDATOR_ALG_NAME;
  }

  protected String getCertPathAlgorithm() {
    return Const.X509_ALG_NAME;
  }

  /**
   * Gets a Certificate from encoded DER bytes.
   *
   * @param cf  A certificate factory.
   * @param der A der encoded certificate
   * @return The Certificate.
   * @throws CertificateException If the DER value is not a valid certificate.
   */
  public Certificate getCertificate(CertificateFactory cf, byte[] der)
      throws CertificateException {
    final ByteArrayInputStream in = new ByteArrayInputStream(der);
    return cf.generateCertificate(in);
  }

  /**
   * Gets certificate path from the COSE chain.
   *
   * @param chain A COSE chain.
   * @return The Certificate path.
   * @throws CertificateException If the chain is not a valid set of certificates.
   */
  public CertPath getCertPath(Composite chain) throws CertificateException {

    final CertificateFactory cf = CertificateFactory
        .getInstance(getCertPathAlgorithm());

    final List<Certificate> list = new ArrayList<>();
    for (int i = 0; i < chain.size(); i++) {
      list.add(getCertificate(cf, chain.getAsBytes(i)));
    }

    return cf.generateCertPath(list);

  }

  /**
   * Gets the certificate path parameters use for verification.
   *
   * @param anchors The trusted anchors used to verify the path.
   * @return The certificate path parameters.
   * @throws InvalidAlgorithmParameterException If the parameters are not valid.
   */
  protected CertPathParameters getCertPathParameters(Set<TrustAnchor> anchors)
      throws InvalidAlgorithmParameterException {
    final PKIXParameters params = new PKIXParameters(anchors);
    params.setRevocationEnabled(false);
    return params;
  }

  /**
   * Creates a key pair.
   *
   * @param algName The Keypair algorithm.
   * @return the public/private key pair.
   */
  public KeyPair createKeyPair(String algName) {

    if (algName.equals(Const.SECP256R1_CURVE_NAME)
        || algName.equals(Const.SECP384R1_CURVE_NAME)) {

      try {
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(algName);
        KeyPairGenerator kg = KeyPairGenerator.getInstance(Const.EC_ALG_NAME);
        kg.initialize(ecSpec, getSecureRandom());
        return kg.generateKeyPair();
      } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
        throw new CryptoServiceException(e);
      }
      //} else if (algName.equals("")) {
      //
    }
    throw new CryptoServiceException(new NoSuchAlgorithmException(algName));
  }

  /**
   * Writes the length value to the output stream.
   *
   * @param out The Output stream to write to.
   * @param len The length to write.
   * @throws IOException If an exception occurs while writing the length.
   */
  protected void writeLen(OutputStream out, int len) throws IOException {
    out.write((byte) (len >> 8));
    out.write((byte) len);
  }

  /**
   * Decode a ECDH Message.
   *
   * <p>This is a 3 part encoded message with each part length prefixed.</p>
   * <p>bstr[blen(Ax), Ax, blen(Ay), Ay, blen(OwnerRandom), OwnerRandom]</p>
   * <p>bstr[blen(Bx), Bx, blen(By), By, blen(DeviceRandom), DeviceRandom]</p>
   *
   * @param message The 3 Part message.
   * @return A Composite with all 3 parts decoded.
   */
  protected Composite decodeEcdhMessage(byte[] message) {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(message)) {
      Composite result = Composite.newArray();

      for (int i = 0; i < Const.FOURTH_KEY; i++) {

        //get the size of the three part message
        int len = (short) (((bis.read() & 0xFF) << 8) | (bis.read() & 0xFF));
        if (len < 0 || len >= message.length) {
          throw new IllegalArgumentException();
        }
        byte[] value = new byte[len];
        bis.read(value);
        result.set(result.size(), value);
      }
      return result;
    } catch (IOException e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * Gets the ECDH Key Exchange message.
   *
   * @param curveName  The EC Curve to use.
   * @param randomSize The size of the random data.
   * @param party      The party to the exchange ("A" or "B").
   * @return A composite with state information.
   */
  protected Composite getEcdhMessage(String curveName, int randomSize, String party) {

    final KeyPair kp = createKeyPair(curveName);
    final ECPublicKey publicKey = (ECPublicKey) kp.getPublic();
    final int bitLength = publicKey.getParams().getCurve().getField().getFieldSize();
    final int byteLength = bitLength / Byte.SIZE;
    final byte[] randomBytes = getRandomBytes(randomSize);

    //bstr[blen(x), x, blen(y), y, blen(Random), Random]
    final byte[] x = adjustBigBuffer(publicKey.getW().getAffineX().toByteArray(), byteLength);
    final byte[] y = adjustBigBuffer(publicKey.getW().getAffineY().toByteArray(), byteLength);

    try (ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      writeLen(bao, x.length);
      bao.writeBytes(x);
      writeLen(bao, y.length);
      bao.writeBytes(y);
      writeLen(bao, randomBytes.length);
      bao.writeBytes(randomBytes);
      return Composite.newArray()
          .set(Const.FIRST_KEY, bao.toByteArray())
          .set(Const.SECOND_KEY, Const.ECDH_ALG_NAME)
          .set(Const.THIRD_KEY, kp.getPrivate().getFormat())
          .set(Const.FOURTH_KEY, kp.getPrivate().getEncoded())
          .set(Const.FIFTH_KEY, party);
    } catch (IOException ioException) {
      throw new CryptoServiceException(ioException);
    }
  }

  protected byte[] getEcdhSharedSecret(byte[] message, Composite ownState) {
    try {
      final Composite theirState = decodeEcdhMessage(message);
      final byte[] theirX = theirState.getAsBytes(Const.FIRST_KEY);
      final byte[] theirY = theirState.getAsBytes(Const.SECOND_KEY);
      final byte[] theirRandom = theirState.getAsBytes(Const.THIRD_KEY);

      final byte[] encoded = ownState.getAsBytes(Const.FOURTH_KEY);
      final PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(encoded);
      final KeyFactory factory = getKeyFactoryInstance(Const.EC_ALG_NAME);
      final ECPrivateKey ownKey = (ECPrivateKey) factory.generatePrivate(privateSpec);

      final ECPoint w = new ECPoint(new BigInteger(1, theirX), new BigInteger(1, theirY));
      final ECPublicKeySpec publicSpec = new ECPublicKeySpec(w, ownKey.getParams());

      final PublicKey theirKey = factory.generatePublic(publicSpec);

      final KeyAgreement keyAgreement = getKeyAgreementInstance(Const.ECDH_ALG_NAME);
      keyAgreement.init(ownKey);
      keyAgreement.doPhase(theirKey, true);
      final byte[] sharedSecret = keyAgreement.generateSecret();

      final Composite ownMessage = decodeEcdhMessage(ownState.getAsBytes(Const.FIRST_KEY));
      final byte[] ownRandom = ownMessage.getAsBytes(Const.THIRD_KEY);
      final ByteBuffer buffer = ByteBuffer
          .allocate(sharedSecret.length + ownRandom.length + theirRandom.length);
      buffer.put(sharedSecret);
      final String party = ownState.getAsString(Const.FIFTH_KEY);
      if (party.equals(Const.KEY_EXCHANGE_A)) {
        //A is owner
        buffer.put(theirRandom);
        buffer.put(ownRandom);
      } else if (party.equals(Const.KEY_EXCHANGE_B)) {
        //B is device
        buffer.put(ownRandom);
        buffer.put(theirRandom);
      } else {
        throw new IllegalArgumentException();
      }

      buffer.flip();
      return Composite.unwrap(buffer);
    } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * Gets the key exchange shared secrete.
   *
   * @param message  The key exchange message.
   * @param ownState The state of the key exchange.
   * @param decryptionKey The decryption key for use in asymmetric exchanges
   * @return The shared secrete based on the state and message.
   */
  public byte[] getSharedSecret(byte[] message, Composite ownState, Key decryptionKey) {

    final String alg = ownState.getAsString(Const.SECOND_KEY);
    switch (alg) {
      case Const.ECDH_ALG_NAME:
        return getEcdhSharedSecret(message, ownState);
      case Const.ASYMKEX2048_ALG_NAME:
      case Const.ASYMKEX3072_ALG_NAME:
        return getAsymkexSharedSecret(message, ownState, decryptionKey);
      default:
        throw new CryptoServiceException(new NoSuchAlgorithmException(alg));
    }
  }

  /**
   * Gets the Key Exchange message.
   * <p>Creates ExchangeA if called on the server</p>
   * <p>Creates ExchangeB if called on the device</p>
   *
   * @param kexSuiteName The name of the Key Exchange Suite.
   * @param party        The party to the Key Ecxhange (A or B)
   * @param ownerKey     The owner key, required for some asymmetric exchanges
   * @return A composite state value with the fist key set to the message.
   */
  public Composite getKeyExchangeMessage(String kexSuiteName, String party, Key ownerKey) {

    switch (kexSuiteName) {
      case Const.ECDH_ALG_NAME:
        return getEcdhMessage(Const.SECP256R1_CURVE_NAME, Const.ECDH_256_RANDOM_SIZE, party);
      case Const.ECDH384_ALG_NAME:
        return getEcdhMessage(Const.SECP384R1_CURVE_NAME, Const.ECDH_384_RANDOM_SIZE, party);
      case Const.ASYMKEX2048_ALG_NAME:
        return getAsymkexMessage(
            Const.ASYMKEX2048_ALG_NAME, Const.ASYMKEX2048_RANDOM_SIZE, party, ownerKey);
      case Const.ASYMKEX3072_ALG_NAME:
        return getAsymkexMessage(
            Const.ASYMKEX3072_ALG_NAME, Const.ASYMKEX3072_RANDOM_SIZE, party, ownerKey);
      default:
        throw new CryptoServiceException(new NoSuchAlgorithmException(kexSuiteName));
    }
  }

  protected Composite getAsymkexMessage(
      String algName, int randomSize, String party, Key encryptionKey) {

    switch (party) {

      case Const.KEY_EXCHANGE_A:

        byte[] a = new byte[randomSize];
        getSecureRandom().nextBytes(a);

        return Composite.newArray()
            .set(Const.FIRST_KEY, a)
            .set(Const.SECOND_KEY, algName)
            .set(Const.THIRD_KEY, Const.KEY_EXCHANGE_A)
            .set(Const.FOURTH_KEY, randomSize);

      case Const.KEY_EXCHANGE_B:

        byte[] b = new byte[randomSize];
        getSecureRandom().nextBytes(b);

        byte[] xb;
        try {
          Cipher cipher = Cipher.getInstance(Const.ASYMKEX_CIPHER_NAME, BCPROV);
          cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, getSecureRandom());
          xb = cipher.doFinal(b);
        } catch (GeneralSecurityException e) {
          throw new RuntimeException(e);
        }

        return Composite.newArray()
            .set(Const.FIRST_KEY, xb)
            .set(Const.SECOND_KEY, algName)
            .set(Const.THIRD_KEY, Const.KEY_EXCHANGE_B)
            .set(Const.FOURTH_KEY, randomSize)
            .set(Const.FIFTH_KEY, b);

      default:
        throw new IllegalArgumentException(party);
    }
  }

  protected byte[] getAsymkexSharedSecret(byte[] message, Composite ownState, Key decryptionKey) {

    String party = ownState.getAsString(Const.THIRD_KEY);
    int len = ownState.getAsNumber(Const.FOURTH_KEY).intValue();

    byte[] shSe = new byte[2 * len];
    ByteBuffer bb = ByteBuffer.wrap(shSe);

    switch (party) {
      case Const.KEY_EXCHANGE_A:

        bb.put(ownState.getAsBytes(Const.FIRST_KEY));

        byte[] b;
        try {
          Cipher cipher = Cipher.getInstance(Const.ASYMKEX_CIPHER_NAME, BCPROV);
          cipher.init(Cipher.DECRYPT_MODE, decryptionKey, getSecureRandom());
          b = cipher.doFinal(message);
        } catch (GeneralSecurityException e) {
          throw new RuntimeException(e);
        }
        bb.put(b);
        break;

      case Const.KEY_EXCHANGE_B:
        bb.put(message);
        bb.put(ownState.getAsBytes(Const.FIFTH_KEY));
        break;

      default:
        throw new IllegalArgumentException(party);
    }

    return shSe;
  }

  protected Composite encryptThenMac(byte[] secret, byte[] ciphered, byte[] iv, String cipherName) {

    int aesType;
    if (cipherName.equals(Const.AES128_CTR_HMAC256_ALG_NAME)) {
      aesType = Const.ETM_AES128_CTR;
    } else if (cipherName.equals(Const.AES128_CBC_HMAC256_ALG_NAME)) {
      aesType = Const.ETM_AES128_CBC;
    } else if (cipherName.equals(Const.AES256_CBC_HMAC384_ALG_NAME)) {
      aesType = Const.ETM_AES256_CBC;
    } else if (cipherName.equals(Const.AES256_CTR_HMAC384_ALG_NAME)) {
      aesType = Const.ETM_AES256_CTR;
    } else {
      throw new CryptoServiceException(new NoSuchAlgorithmException());
    }

    byte[] protectedHeader = Composite.newMap()
        .set(Const.ETM_AES_PLAIN_TYPE, aesType)
        .toBytes();
    Composite cose0 = Composite.newArray()
        .set(Const.COSE_SIGN1_PROTECTED, protectedHeader)
        .set(Const.COSE_SIGN1_UNPROTECTED,
            Composite.newMap()
                .set(Const.ETM_AES_IV, iv))
        .set(Const.COSE_SIGN1_PAYLOAD, ciphered);

    int hmacType;
    if (cipherName.equals(Const.AES128_CTR_HMAC256_ALG_NAME)
        || cipherName.equals(Const.AES128_CBC_HMAC256_ALG_NAME)) {
      hmacType = Const.HMAC_SHA_256;
    } else if (cipherName.equals(Const.AES256_CBC_HMAC384_ALG_NAME)
        || cipherName.equals(Const.AES256_CTR_HMAC384_ALG_NAME)) {
      hmacType = Const.HMAC_SHA_384;
    } else {
      throw new CryptoServiceException(new NoSuchAlgorithmException());
    }
    byte[] payload = cose0.toBytes();
    Composite mac = hash(hmacType, secret, payload);

    protectedHeader = Composite.newMap()
        .set(Const.ETM_MAC_TYPE, hmacType)
        .toBytes();
    Composite mac0 = Composite.newArray()
        .set(Const.COSE_SIGN1_PROTECTED, protectedHeader)
        .set(Const.COSE_SIGN1_UNPROTECTED, Composite.newMap())
        .set(Const.COSE_SIGN1_PAYLOAD, payload)
        .set(Const.COSE_SIGN1_SIGNATURE, mac.getAsBytes(Const.HASH));

    return mac0;
  }

  protected Composite getSmallerKdf(byte[] shSe) {
    //HMAC-SHA-256[0,(byte)1||"FIDO-KDF"||(byte)0||"AutomaticOnboard-cipher"||ShSe]
    ByteBuffer buffer = ByteBuffer
        .allocate(1 + Const.KDF_STRING.length + 1 + Const.PROV_CIPHER.length + shSe.length);
    buffer.put((byte) 1);
    buffer.put(Const.KDF_STRING);
    buffer.put((byte) 0);
    buffer.put(Const.PROV_CIPHER);
    buffer.put(shSe);
    buffer.flip();
    Composite hash = hash(Const.HMAC_SHA_256, Const.HMAC_ZERO, Composite.unwrap(buffer));
    buffer = ByteBuffer.wrap(hash.getAsBytes(Const.HASH));
    byte[] sek = new byte[(Const.BIT_LEN_256 / Byte.SIZE) / 2];//16 bytes
    buffer.get(sek);

    //HMAC-SHA-256[0,(byte)2||"FIDO-KDF"||(byte)0||"AutomaticOnboard-hmac"||ShSe]
    buffer = ByteBuffer
        .allocate(1 + Const.KDF_STRING.length + 1 + Const.PROV_HMAC.length + shSe.length);

    buffer.put((byte) 2);
    buffer.put(Const.KDF_STRING);
    buffer.put((byte) 0);
    buffer.put(Const.PROV_HMAC);
    buffer.put(shSe);
    buffer.flip();

    hash = hash(Const.HMAC_SHA_256, Const.HMAC_ZERO, Composite.unwrap(buffer));
    buffer = ByteBuffer.wrap(hash.getAsBytes(Const.HASH));
    byte[] svk = new byte[Const.BIT_LEN_256 / Byte.SIZE]; //32 bytes
    buffer.get(svk);

    return Composite.newArray()
        .set(Const.FIRST_KEY, sek)
        .set(Const.SECOND_KEY, svk);
  }

  protected Composite getLargerKdf(byte[] shSe) {
    //HMAC-SHA-256[0,(byte)1||"FIDO-KDF"||(byte)0||"AutomaticOnboard-cipher"||ShSe]
    ByteBuffer buffer = ByteBuffer
        .allocate(1 + Const.KDF_STRING.length + 1 + Const.PROV_CIPHER.length + shSe.length);
    buffer.put((byte) 1);
    buffer.put(Const.KDF_STRING);
    buffer.put((byte) 0);
    buffer.put(Const.PROV_CIPHER);
    buffer.put(shSe);
    buffer.flip();
    Composite hash = hash(Const.HMAC_SHA_384, Const.HMAC_ZERO, Composite.unwrap(buffer));
    buffer = ByteBuffer.wrap(hash.getAsBytes(Const.HASH));
    byte[] sek = new byte[Const.BIT_LEN_256 / Byte.SIZE]; // 32 bytes
    buffer.get(sek);

    //HMAC-SHA-256[0,(byte)2||"FIDO-KDF"||(byte)0||"AutomaticOnboard-hmac"||ShSe]
    buffer = ByteBuffer
        .allocate(1 + Const.KDF_STRING.length + 1 + Const.PROV_HMAC.length + shSe.length);

    buffer.put((byte) 2);
    buffer.put(Const.KDF_STRING);
    buffer.put((byte) 0);
    buffer.put(Const.PROV_HMAC);
    buffer.put(shSe);
    buffer.flip();
    hash = hash(Const.HMAC_SHA_384, Const.HMAC_ZERO, Composite.unwrap(buffer));
    buffer = ByteBuffer.wrap(hash.getAsBytes(Const.HASH));
    byte[] svk1 = new byte[Const.BIT_LEN_384 / Byte.SIZE]; //48 bytes
    buffer.get(svk1);

    //HMAC-SHA-256[0,(byte)3||"FIDO-KDF"||(byte)0||"AutomaticOnboard-hmac"||ShSe]
    buffer = ByteBuffer
        .allocate(1 + Const.KDF_STRING.length + 1 + Const.PROV_HMAC.length + shSe.length);
    buffer.put((byte) 3);
    buffer.put(Const.KDF_STRING);
    buffer.put((byte) 0);
    buffer.put(Const.PROV_HMAC);
    buffer.put(shSe);
    buffer.flip();
    hash = hash(Const.HMAC_SHA_384, Const.HMAC_ZERO, Composite.unwrap(buffer));
    buffer = ByteBuffer.wrap(hash.getAsBytes(Const.HASH));
    byte[] svk2 = new byte[(Const.BIT_LEN_256 / Byte.SIZE) / 2];//16 bytes
    buffer.get(svk2);

    buffer = ByteBuffer.allocate(svk1.length + svk2.length);
    buffer.put(svk1);
    buffer.put(svk2);
    buffer.flip();

    return Composite.newArray()
        .set(Const.FIRST_KEY, sek)
        .set(Const.SECOND_KEY, Composite.unwrap(buffer)); //svk
  }

  protected byte[] getCtrIv() {
    ByteBuffer buffer = ByteBuffer.allocate(Const.IV_SIZE);
    byte[] seed = getRandomBytes(Const.IV_SEED_SIZE);
    buffer.put(seed);
    buffer.put(new byte[]{0, 0, 0, 0});
    buffer.flip();
    return Composite.unwrap(buffer);
  }

  protected void updateIv(int cipheredLen, Composite state) {
    final String cipherName = state.getAsString(Const.FIRST_KEY);
    final ByteBuffer iv = state.getAsByteBuffer(Const.THIRD_KEY);
    long counter = state.getAsNumber(Const.FOURTH_KEY).longValue();
    if (cipherName.indexOf("/CTR/") > 0) {
      //the last 4 bytes of the iv will be the counter
      byte[] seed = new byte[Const.IV_SEED_SIZE];
      iv.get(seed);

      int blockCount = 1 + (cipheredLen - 1) / Const.IV_SIZE;
      counter += blockCount;

      ByteBuffer buffer = ByteBuffer.allocate(Const.IV_SIZE);
      buffer.put(seed);

      buffer.putInt((int) counter);
      buffer.flip();

      state.set(Const.THIRD_KEY, Composite.unwrap(buffer));
      state.set(Const.FOURTH_KEY, counter);
    } else if (cipherName.indexOf("/CBC/") > 0) {
      byte[] seed = new byte[Const.IV_SEED_SIZE];
      iv.get(seed);

      byte[] rnd = getRandomBytes(4);

      ByteBuffer buffer = ByteBuffer.allocate(Const.IV_SIZE);
      buffer.put(seed);
      buffer.put(rnd);
      buffer.flip();
      state.set(Const.THIRD_KEY, Composite.unwrap(buffer));
    } else {
      throw new CryptoServiceException(new NoSuchAlgorithmException());
    }
  }

  /**
   * Decrypts a message.
   *
   * @param message The ciphered message.
   * @param state   The crypto state.
   * @return The decrypted message.
   */
  public byte[] decrypt(Composite message, Composite state) {
    try {
      final Cipher cipher = getCipherInstance(state.getAsString(Const.FIRST_KEY));
      final Composite keys = state.getAsComposite(Const.SECOND_KEY);
      //final byte[] iv = state.getAsBytes(Const.THIRD_KEY);
      final byte[] sek = keys.getAsBytes(Const.FIRST_KEY);
      final byte[] svk = keys.getAsBytes(Const.SECOND_KEY);
      final Key keySpec = new SecretKeySpec(sek, Const.AES);

      final Composite cose0 = Composite.fromObject(message.getAsBytes(Const.COSE_SIGN1_PAYLOAD));

      final Composite uph = cose0.getAsComposite(Const.COSE_SIGN1_UNPROTECTED);
      final byte[] iv = uph.getAsBytes(Const.ETM_AES_IV);

      cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
      final byte[] ciphered = cose0.getAsBytes(Const.COSE_SIGN1_PAYLOAD);
      final byte[] mac1 = message.getAsBytes(Const.COSE_SIGN1_SIGNATURE);
      final byte[] protectedHeader = message.getAsBytes(Const.COSE_SIGN1_PROTECTED);
      final Composite prh = Composite.fromObject(protectedHeader);
      int macType = prh.getAsNumber(Const.ETM_MAC_TYPE).intValue();

      Composite mac2 = hash(macType, svk, cose0.toBytes());
      this.verifyBytes(mac1, mac2.getAsBytes(Const.HASH));

      return cipher.doFinal(ciphered);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException
        | InvalidAlgorithmParameterException | InvalidKeyException
        | BadPaddingException | IllegalBlockSizeException e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * Encrypts a message.
   *
   * @param payload The payload to encrypt.
   * @param state   The saved crypto state.
   * @return The encrypted message and state.
   */
  public Composite encrypt(byte[] payload, Composite state) {
    try {
      final String cipherName = state.getAsString(Const.FIRST_KEY);
      final Cipher cipher = getCipherInstance(cipherName);
      final Composite keys = state.getAsComposite(Const.SECOND_KEY);
      final byte[] iv = state.getAsBytes(Const.THIRD_KEY);
      final byte[] sek = keys.getAsBytes(Const.FIRST_KEY);
      final byte[] sev = keys.getAsBytes(Const.SECOND_KEY);
      final Key keySpec = new SecretKeySpec(sek, Const.AES);

      cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));

      final byte[] ciphered = cipher.doFinal(payload);

      Composite message = encryptThenMac(sev, ciphered, iv, cipherName);
      updateIv(ciphered.length, state);
      return message;

    } catch (NoSuchPaddingException | NoSuchAlgorithmException
        | InvalidAlgorithmParameterException | InvalidKeyException
        | BadPaddingException | IllegalBlockSizeException e) {
      throw new CryptoServiceException(e);
    }
  }

  /**
   * gets the the encryption state.
   *
   * @param shSe        Shared secret.
   * @param cipherSuite The cipher suite to use.
   * @return A Composite encryption state.
   */
  public Composite getEncryptionState(byte[] shSe, String cipherSuite) {

    final Composite state = Composite.newArray();

    if (cipherSuite.equals(Const.AES128_CTR_HMAC256_ALG_NAME)) {
      state.set(Const.FIRST_KEY, Const.AES128_CTR_HMAC256_ALG_NAME);
      state.set(Const.SECOND_KEY, getSmallerKdf(shSe));
      state.set(Const.THIRD_KEY, getCtrIv());
      state.set(Const.FOURTH_KEY, 0L);
    } else if (cipherSuite.equals(Const.AES128_CBC_HMAC256_ALG_NAME)) {
      state.set(Const.FIRST_KEY, Const.AES128_CBC_HMAC256_ALG_NAME);
      state.set(Const.SECOND_KEY, getSmallerKdf(shSe));
    } else if (cipherSuite.equals(Const.AES256_CTR_HMAC384_ALG_NAME)) {
      state.set(Const.FIRST_KEY, Const.AES256_CTR_HMAC384_ALG_NAME);
      state.set(Const.SECOND_KEY, getLargerKdf(shSe));
      state.set(Const.THIRD_KEY, getCtrIv());
      state.set(Const.FOURTH_KEY, 0L);
    } else if (cipherSuite.equals(Const.AES256_CBC_HMAC384_ALG_NAME)) {
      state.set(Const.FIRST_KEY, Const.AES256_CBC_HMAC384_ALG_NAME);
      state.set(Const.SECOND_KEY, getLargerKdf(shSe));
    } else {
      throw new CryptoServiceException(new NoSuchAlgorithmException(cipherSuite));
    }
    return state;
  }

  /**
   * Gets a UEID from a device guid.
   *
   * @param guid The device guid.
   * @return A COSE UEID.
   */
  public byte[] getUeidFromGuid(byte[] guid) {
    ByteBuffer buf = ByteBuffer.allocate(guid.length + 1);
    buf.put((byte) Const.EAT_RAND);
    buf.put(guid);
    buf.flip();
    return Composite.unwrap(buf);
  }

  /**
   * Gets the device guid from a COSE UEID.
   *
   * @param ueid the cose UEID.
   * @return The device GUID as a UUID.
   */
  public UUID getGuidFromUeid(byte[] ueid) {

    if (ueid.length != Const.GUID_SIZE + 1) {
      throw new InvalidMessageException();
    }
    if (ueid[0] != Const.EAT_RAND) {
      throw new InvalidMessageException();
    }

    byte[] guid = new byte[Const.GUID_SIZE];
    System.arraycopy(ueid, 1, guid, 0, guid.length);

    return Composite.newArray()
        .set(Const.FIRST_KEY, guid)
        .getAsUuid(Const.FIRST_KEY);
  }

  /**
   * Gets the device public key from the voucher.
   *
   * @param voucher An ownership voucher.
   * @return The device public key.
   */

  public PublicKey getDevicePublicKey(Composite voucher) {
    Object chain = voucher.get(Const.OV_DEV_CERT_CHAIN);
    if (chain != null) {
      Composite certs = Composite.fromObject(chain);
      if (certs.size() == 0) {
        return null; // no cert chain so most likely a MAROE EPID device
      }
      try {
        CertPath path = getCertPath(certs);
        return path.getCertificates().get(0).getPublicKey();
      } catch (CertificateException e) {
        throw new DispatchException(e);
      }
    }
    return null;
  }

  /**
   * Gets the public key from the voucher.
   *
   * @param voucher A ownership voucher.
   * @return The last owners public key.
   */
  public Composite getOwnerPublicKey(Composite voucher) {
    Composite header = voucher.getAsComposite(Const.OV_HEADER);
    Composite pub = header.getAsComposite(Const.OVH_PUB_KEY);
    Composite entries = voucher.getAsComposite(Const.OV_ENTRIES);
    //can be zero
    if (entries.size() > 0) {
      Composite entry = entries.getAsComposite(entries.size() - 1);
      Composite payload = Composite.fromObject(entry.getAsBytes(Const.COSE_SIGN1_PAYLOAD));
      pub = payload.getAsComposite(Const.OVE_PUB_KEY);
    }
    return pub;
  }

  /**
   * Gets the MD5 fingerprint for the public key.
   *
   * @param publicKey The public key.
   * @return The MD5 fingerprint.
   */
  public String getFingerPrint(PublicKey publicKey) {
    Composite encKey = encode(publicKey, Const.PK_ENC_X509);
    byte[] derBytes = encKey.getAsBytes(Const.PK_BODY);
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new CryptoServiceException(e);
    }
    byte[] digest = messageDigest.digest(derBytes);
    return Composite.toString(digest);
  }
}
