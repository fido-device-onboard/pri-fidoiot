package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.NoSuchElementException;
import org.fidoalliance.fdo.protocol.message.CoseKeyCurveType;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

public class AlgorithmFinder {


  /**
   * Gets the Java hash Algorithm for the given HashType
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
        throw new NoSuchElementException();
    }
  }

  /**
   * Get the Digest/SHA HashType for a given HMAC Type.
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
        throw new NoSuchElementException();
    }
  }

  /**
   * Get the Digest/SHA HashType for a given KeySize.
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
        throw new NoSuchElementException();
    }
  }


  /**
   * Gets the Java Algorithm for the provided key
   * @param keyType The provided Public key Type.
   * @return The Java Key Factory Algorithm.
   */
  public String getAlgorithm(PublicKeyType keyType)  {
    switch (keyType) {
      case RSA2048RESTR:
      case RSAPKCS:
        return "RSA";
      case SECP256R1:
      case SECP384R1:
        return "EC";
      default:
        throw new NoSuchElementException();
    }
  }


  /**
   * Gets the Java algorithm for the given CoseKey Curve.
   * @param coseKeyCurveType
   * @return The Java algorithm name.
   * @throws IOException The Algorithm is not supported.
   */
  public String getAlgorithm(CoseKeyCurveType coseKeyCurveType)  {
    switch ( coseKeyCurveType) {
      case P256EC2:
        return "secp256r1";
      case P384EC2:
        return "secp384r1";
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * Gets the CoseKey Curve type for the given public Key Type.
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
        throw new IllegalArgumentException();
    }
  }

  /**
   * Gets the Java Signature Algorithm for the give key type and Size.
   * @param keyType The java key size.
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
        throw new IllegalArgumentException();
      case SECP256R1:
        return "SHA256withECDSA";
      case SECP384R1:
        return "SHA384withECDSA";
      default:
        throw new IllegalArgumentException();
    }
  }


  /**
   * Gets the keysize of the provided key.
   *
   * @param pubKey A public key.
   * @return The size of the key.
   */
  public KeySizeType getKeySizeType(PublicKey pubKey)  {
    if (pubKey instanceof ECPublicKey) {
      final ECPublicKey ec = (ECPublicKey) pubKey;
      return KeySizeType.fromNumber(ec.getParams().getCurve().getField().getFieldSize());
    } else if (pubKey instanceof RSAPublicKey) {
      final RSAPublicKey rsa = (RSAPublicKey) pubKey;
      return KeySizeType.fromNumber(rsa.getModulus().bitLength());
    }
    throw new IllegalArgumentException();
  }
}
