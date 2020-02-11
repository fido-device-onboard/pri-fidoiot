// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Key-related utilities.
 */
class Keys {

  private static final Logger LOG = LoggerFactory.getLogger(Keys.class);
  private static final Integer RSA2048RESTR_MAX_BITS = 2048;

  /**
   * Return the size of this key, in bytes.
   */
  static int sizeInBytes(Key key) {

    if (key instanceof ECKey) {
      ECKey ecKey = (ECKey) key;
      return ecKey.getParams().getCurve().getField().getFieldSize() / Byte.SIZE;

    } else if (key instanceof RSAKey) {
      RSAKey rsaKey = (RSAKey) key;
      return rsaKey.getModulus().bitLength() / Byte.SIZE;
    }

    throw new IllegalArgumentException(key != null ? key.getAlgorithm() : null);
  }

  /**
   * Converts a {@link KeySpec} to its corresponding {@link PublicKey}.
   */
  static PublicKey toPublicKey(KeySpec keySpec) throws NoSuchAlgorithmException {

    for (String algorithm : new String[]{"EC", "RSA", "EPID"}) {

      LOG.debug("trying to decode unknown keyspec as " + algorithm + "...");

      try {
        KeyFactory factory = KeyFactory.getInstance(algorithm, BouncyCastleLoader.load());
        PublicKey key = factory.generatePublic(keySpec);
        LOG.debug("key recognized as " + algorithm);
        return key;

      } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
        // failures are expected, try the next algorithm
        LOG.debug("key not recognized as " + algorithm + ", trying next...");
      }
    }

    throw new NoSuchAlgorithmException(keySpec.toString());
  }

  /**
   * Identify the {@link KeyType} of the given {@link PublicKey}.
   */
  static KeyType toType(PublicKey key) {

    if (null == key) {
      return KeyType.NONE;

    } else if (key instanceof ECPublicKey) {
      final ECPublicKey ecKey = (ECPublicKey) key;
      final ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(ecKey.getEncoded());

      // The curve parameters can be direct or indirect.
      //
      // If it's indirect,
      // the key will contain an OID identifying the curve parameters.
      //
      // If it's direct,
      // the key will contain the curve parameters, which we must match
      // against a known curve.
      //
      for (final ASN1ObjectIdentifier oid : findObjectIdentifiers(asn1Sequence)) {
        if (SECObjectIdentifiers.secp256r1.equals(oid)) {
          return KeyType.ECDSA_P_256;

        } else if (SECObjectIdentifiers.secp384r1.equals(oid)) {
          return KeyType.ECDSA_P_384;

        } else if (X9ObjectIdentifiers.prime_field.equals(oid)) {

          final ECParameterSpec secp256r1 =
              EC5Util.convertToSpec(SECNamedCurves.getByOID(SECObjectIdentifiers.secp256r1));
          final ECParameterSpec secp384r1 =
              EC5Util.convertToSpec(SECNamedCurves.getByOID(SECObjectIdentifiers.secp384r1));

          if (areEqualByValue(secp256r1, ecKey.getParams())) {
            return KeyType.ECDSA_P_256;

          } else if (areEqualByValue(secp384r1, ecKey.getParams())) {
            return KeyType.ECDSA_P_384;

          }
        }
      }

    } else if (key instanceof RSAPublicKey) {
      RSAPublicKey rsaKey = (RSAPublicKey) key;

      if (RSAKeyGenParameterSpec.F4.equals(rsaKey.getPublicExponent())
          && rsaKey.getModulus().bitLength() <= RSA2048RESTR_MAX_BITS) {
        return KeyType.RSA2048RESTR;

      } else {
        return KeyType.RSA_UR;
      }

    } else if (key instanceof EpidKey10) {
      return KeyType.EPIDV1_0;
    } else if (key instanceof EpidKey11) {
      return KeyType.EPIDV1_1;
    } else if (key instanceof EpidKey20) {
      return KeyType.EPIDV2_0;
    } else if (key instanceof EpidKey) {
      return KeyType.EPIDV2_0;
    }

    throw new UnsupportedOperationException(key.getClass().toString());
  }

  // ECParameterSpec types only support equality by identity, but we need
  // to be able to check for value-equality.
  private static boolean areEqualByValue(final ECParameterSpec left, final ECParameterSpec right) {

    return !(null == left || null == right)
        && left.getCurve().equals(right.getCurve())
        && left.getCofactor() == right.getCofactor()
        && left.getGenerator().equals(right.getGenerator())
        && left.getOrder().equals(right.getOrder());
  }

  private static Set<ASN1ObjectIdentifier> findObjectIdentifiers(
      final ASN1Encodable asn1Encodable) {

    if (asn1Encodable instanceof ASN1ObjectIdentifier) {
      return Set.of((ASN1ObjectIdentifier) asn1Encodable);

    } else if (asn1Encodable instanceof DLSequence) {
      final DLSequence dlSequence = (DLSequence) asn1Encodable;
      final Set<ASN1ObjectIdentifier> s = new HashSet<>();

      dlSequence.forEach(element -> s.addAll(findObjectIdentifiers(element)));
      return s;

    } else {
      return Collections.emptySet();
    }
  }
}
