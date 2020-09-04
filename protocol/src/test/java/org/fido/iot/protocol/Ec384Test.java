// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

public class Ec384Test {

  @Test
  void Test() throws Exception {

    SecureRandom random = new SecureRandom();

    ECGenParameterSpec ecSpec = new ECGenParameterSpec(Const.SECP384R1_CURVE_NAME);
    KeyPairGenerator kg = KeyPairGenerator.getInstance(Const.EC_ALG_NAME);
    kg.initialize(ecSpec, random);
    KeyPair keypair = kg.generateKeyPair();
    PublicKey publicKey = keypair.getPublic();
    PrivateKey privateKey = keypair.getPrivate();

    String pvkData = Base64.getEncoder().encodeToString(privateKey.getEncoded());
    String pvkFormat = privateKey.getFormat();

    String pubData = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    String pubFormat = publicKey.getFormat();

    Provider bc = new BouncyCastleProvider();
    CryptoService service = new CryptoService();

    byte[] payload = service.getRandomBytes(15);

    Composite header = Composite.newMap();
    header.set(Const.COSE_ALG, Const.COSE_ES384);

    Composite pub = service.encode(publicKey, Const.PK_ENC_COSEEC);
    String km = Base64.getEncoder().encodeToString(pub.getAsByteBuffer(Const.PK_BODY).array());

    PublicKey p2 = service.decode(pub);
    assertTrue(service.compare(publicKey, p2) == 0);

    Composite cos = service.sign(privateKey, payload);

    assertTrue(service.verify(publicKey, cos));

    payload = service.getRandomBytes(15);
    cos.set(Const.COSE_SIGN1_PAYLOAD, payload);
    assertFalse(service.verify(publicKey, cos));

  }
}
