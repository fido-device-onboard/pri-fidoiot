// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.security.InvalidParameterException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import org.fidoalliance.fdo.protocol.message.CipherSuiteType;
import org.fidoalliance.fdo.protocol.message.CoseKeyCurveType;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

/**
 * Finds algorithm values.
 */
public class AlgorithmFinder {


  /**
   * Gets the Java hash Algorithm for the given HashType.
   *
   * @param hashType The HashType.
   * @return The Java Algorithm to use.
   */
  public String getAlgorithm(HashType hashType) {
    switch (hashType) {
      case HMAC_SHA384:
        return "HmacSHA384";
      case HMAC_SHA256:
        return "HmacSHA256";
      case SHA256:
        return "SHA-256";
      case SHA384:
        return "SHA-384";
      default:
        throw new InvalidParameterException("hashType " + hashType);
    }
  }

  /**
   * Gets the Java Algorithm for the provided key.
   *
   * @param keyType The provided Public key Type.
   * @return The Java Key Factory Algorithm.
   */
  public String getAlgorithm(PublicKeyType keyType) {
    switch (keyType) {
      case RSA2048RESTR:
      case RSAPKCS:
        return "RSA";
      case SECP256R1:
      case SECP384R1:
        return "EC";
      default:
        throw new InvalidParameterException("PublicKeyType " + keyType);
    }
  }

  /**
   * Gets the Java algorithm for the given cipher suite.
   * @param cipherType The CipherSuiteType.
   * @return The Java algorithm name.
   */
  public String getAlgorithm(CipherSuiteType cipherType) {
    switch (cipherType) {
      case COSE_AES128_CTR:
      case COSE_AES256_CTR:
        return "AES/CTR/NoPadding";
      case COSE_AES128_CBC:
      case COSE_AES256_CBC:
        return "AES/CBC/PKCS7Padding";
      case A128GCM:
      case A256GCM:
        return "AES/GCM/NoPadding";
      default:
        throw new InvalidParameterException("invalid cipher " + cipherType);
    }

  }


  /**
   * Gets the Java algorithm for the given CoseKey Curve.
   *
   * @param coseKeyCurveType A coseCurveType.
   * @return The Java algorithm name.
   */
  public String getAlgorithm(CoseKeyCurveType coseKeyCurveType) {
    switch (coseKeyCurveType) {
      case P256EC2:
        return "secp256r1";
      case P384EC2:
        return "secp384r1";
      default:
        throw new InvalidParameterException("invalid curve " + coseKeyCurveType);
    }
  }



  /**
   * Get the Digest/SHA HashType for a given HMAC Type.
   *
   * @param hashType A HMAC HashType.
   * @return The compatible Digest/SHA HashTYpe.
   */
  public HashType getCompatibleHashType(HashType hashType) {
    switch (hashType) {

      case SHA384:
        return HashType.HMAC_SHA384;
      case SHA256:
        return HashType.HMAC_SHA256;
      case HMAC_SHA256:
        return HashType.SHA256;
      case HMAC_SHA384:
        return HashType.SHA384;
      default:
        throw new InvalidParameterException("hashType " + hashType);
    }
  }

  /**
   * Get the Digest/SHA HashType for a given KeySize.
   *
   * @param publicKey A public key.
   * @return The compatible Digest/SHA HashTYpe.
   */
  public HashType getCompatibleHashType(PublicKey publicKey) {
    KeySizeType sizeType = getKeySizeType(publicKey);

    switch (sizeType) {
      case SIZE_2048:
      case SIZE_256:
        return HashType.SHA256;
      case SIZE_3072:
      case SIZE_384:
        return HashType.SHA384;
      default:
        throw new InvalidParameterException("sizeType " + sizeType);
    }
  }



  /**
   * Gets the CoseKey Curve type for the given public Key Type.
   *
   * @param keyType The EC PublicKey Type.
   * @return The CoseKey Curve Type.
   */
  public CoseKeyCurveType getCoseKeyCurve(PublicKeyType keyType) {
    switch (keyType) {
      case SECP256R1:
        return CoseKeyCurveType.P256EC2;
      case SECP384R1:
        return CoseKeyCurveType.P384EC2;
      default:
        throw new InvalidParameterException("PublicKeyType " + keyType);
    }
  }

  /**
   * Gets the Java Signature Algorithm for the give key type and Size.
   *
   * @param keyType  The java key size.
   * @param sizeType The KeySize.
   * @return The Java Signature algorithm name.
   */
  public String getSignatureAlgorithm(PublicKeyType keyType, KeySizeType sizeType) {
    switch (keyType) {
      case RSA2048RESTR:
        return "SHA256withRSA";
      case RSAPKCS:
        if (sizeType.equals(KeySizeType.SIZE_2048)) {
          return "SHA256withRSA";
        } else if (sizeType.equals(KeySizeType.SIZE_3072)) {
          return "SHA384withRSA";
        }
        throw new InvalidParameterException("KeySizeType " + sizeType);
      case SECP256R1:
        return "SHA256withECDSA";
      case SECP384R1:
        return "SHA384withECDSA";
      default:
        throw new InvalidParameterException("PublicKeyType " + keyType);
    }
  }

  /**
   * Gets the Cose spec Algorithm Id.
   * @return The Algorithm Id from the Cose spec.
   */
  public int getCoseAlgorithmId() {
    return 1;
  }

  /**
   * Gets the Cose Algorithm value for the given Keytype and key size.
   * @param keyType The PublicKeyType.
   * @param sizeType The size of the key.
   * @return The Cose Algorithm value.
   */
  public int getCoseAlgorithm(PublicKeyType keyType, KeySizeType sizeType) {
    // note:
    // EC values come from COSE spec, table 5
    // RSA values come from
    // From https://datatracker.ietf.org/doc/html/draft-ietf-cose-webauthn-algorithms-05
    switch (keyType) {
      case SECP256R1:
        return -7; //COSE spec, table 5
      case SECP384R1:
        return -35; // COSE spec, table 5
      case RSA2048RESTR:
        return -257; // see note above
      case RSAPKCS: {
        switch (sizeType) {
          case SIZE_2048:
            return -257; // see note above
          case SIZE_3072:
            return -258; // see note above
          default:
            throw new InvalidParameterException("KeySizeType " + sizeType);
        }
      }
      default:
        throw new InvalidParameterException("PublicKeyType " + keyType);
    }
  }

  /**
   * Gets the keysize of the provided key.
   *
   * @param pubKey A public key.
   * @return The size of the key.
   */
  public KeySizeType getKeySizeType(PublicKey pubKey) {
    if (pubKey instanceof ECPublicKey) {
      final ECPublicKey ec = (ECPublicKey) pubKey;
      return KeySizeType.fromNumber(ec.getParams().getCurve().getField().getFieldSize());
    } else if (pubKey instanceof RSAPublicKey) {
      final RSAPublicKey rsa = (RSAPublicKey) pubKey;
      return KeySizeType.fromNumber(rsa.getModulus().bitLength());
    }
    throw new InvalidParameterException("Unsupported public key");
  }


  /**
   * Gets the PublicKeyType from the given key.
   * @param publicKey A Java public key.
   * @return The PublicKeyType type.
   */
  public PublicKeyType getPublicKeyType(PublicKey publicKey) {
    if (publicKey instanceof ECPublicKey) {
      switch (getKeySizeType(publicKey)) {
        case SIZE_256:
          return PublicKeyType.SECP256R1;
        case SIZE_384:
          return PublicKeyType.SECP384R1;
        default:
          break;
      }
    }
    throw new InvalidParameterException("Unsupported public key");
  }

}
