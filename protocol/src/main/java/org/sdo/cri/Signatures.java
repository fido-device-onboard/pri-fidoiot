// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;

abstract class Signatures {

  private static final String ECDSA = "ECDSA";
  private static final String RSA = "RSA";
  private static final String SHA = "SHA";
  private static final String WITH = "with";

  static byte[] sign(String text, PrivateKey signingKey)
      throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
    Signature signer =
        Signature.getInstance(algorithmForKey(signingKey), BouncyCastleLoader.load());
    signer.initSign(signingKey);
    signer.update(text.getBytes(StandardCharsets.US_ASCII));
    return signer.sign();
  }

  static boolean verify(String text, byte[] signature, PublicKey verifyingKey)
      throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {

    Signature verifier =
        Signature.getInstance(algorithmForKey(verifyingKey), BouncyCastleLoader.load());
    verifier.initVerify(verifyingKey);
    verifier.update(text.getBytes(StandardCharsets.US_ASCII));
    return verifier.verify(signature);
  }

  private static String algorithmForKey(Key key) {

    if (key instanceof RSAKey) {
      RSAKey rsa = (RSAKey) key;
      int bytes = rsa.getModulus().bitLength() / Byte.SIZE;
      return SHA + bytes + WITH + RSA;

    } else if (key instanceof ECKey) {
      ECKey ec = (ECKey) key;
      int bytes = ec.getParams().getCurve().getField().getFieldSize();
      return SHA + bytes + WITH + ECDSA;

    } else if (key instanceof EpidKey) {
      return key.getAlgorithm();

    } else {
      throw new UnsupportedOperationException(key.getAlgorithm() + "is not supported");
    }
  }
}
