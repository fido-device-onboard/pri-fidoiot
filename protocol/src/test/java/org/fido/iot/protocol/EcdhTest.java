// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

public class EcdhTest {

  @Test
  void Test() throws Exception {

    byte[] dd = Base64.getDecoder().decode(
        "ACDo4f7a63e1oTh5z0u32xgc42TWtnZvufIGRTJu/iT6KAAgjm5HIGvLbW/vqXajrewkC3dVGkwz8lFjSFtyKWq7tAQAEBrNkhftAX3Q4KmxtiP7HmA=");

    SecureRandom random = new SecureRandom();
    Provider bc = new BouncyCastleProvider();
    CryptoService cryptoService = new CryptoService();

    Composite ownerState = cryptoService
        .getKeyExchangeMessage(Const.ECDH_ALG_NAME, Const.KEY_EXCHANGE_A);
    Composite deviceState = cryptoService
        .getKeyExchangeMessage(Const.ECDH_ALG_NAME, Const.KEY_EXCHANGE_B);

    byte[] kexA = ownerState.getAsBytes(Const.FIRST_KEY);
    byte[] kexB = deviceState.getAsBytes(Const.FIRST_KEY);

    byte[] ownSecret = cryptoService.getSharedSecret(kexA, deviceState);
    byte[] devSecret = cryptoService.getSharedSecret(kexB, ownerState);

    if (ByteBuffer.wrap(ownSecret).compareTo(ByteBuffer.wrap(devSecret)) != 0) {
      throw new RuntimeException("Shared secret does not match");
    }

    Composite state = cryptoService
        .getEncryptionState(devSecret, Const.AES128_CTR_HMAC256_ALG_NAME);

    byte[] payload = "Hello world".getBytes(StandardCharsets.US_ASCII);

    Composite message = cryptoService.encrypt(payload, state);

    byte[] deciphered = cryptoService.decrypt(message, state);

  }

}
