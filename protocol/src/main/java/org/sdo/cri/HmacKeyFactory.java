// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

class HmacKeyFactory implements Supplier<SecretKey> {

  private final KeyType myDeviceAttestationType;
  private final SecureRandom mySecureRandom;

  HmacKeyFactory(PublicKey publicKey, SecureRandom secureRandom) {
    this.myDeviceAttestationType = attestationType(publicKey);
    this.mySecureRandom = Objects.requireNonNull(secureRandom, "secureRandom must be non-null");
  }

  private static KeyType attestationType(PublicKey publicKey) {

    Objects.requireNonNull(publicKey, "publicKey must be non-null");
    KeyType attestationType = Keys.toType(publicKey);
    switch (attestationType) {

      case RSA2048RESTR:
      case RSA_UR:
      case ECDSA_P_256:
      case ECDSA_P_384:
        return attestationType;

      default:
        throw new IllegalArgumentException(
            "attestation (public key) must be ECDSA_P_256, ECDSA_P_384, RSA2048RESTR, or RSA_UR");
    }
  }

  @Override
  public SecretKey get() {
    final int secretLength;
    final String algo;
    switch (myDeviceAttestationType) {
      case RSA2048RESTR:
      case ECDSA_P_256:
        secretLength = 128 / 8; // see protocol spec appendix C
        algo = "HmacSHA256";
        break;
      case ECDSA_P_384:
      case RSA_UR:
        secretLength = 512 / 8; // see protocol spec appendix C
        algo = "HmacSHA384";
        break;
      default:
        throw new RuntimeException("unsupported key type");
    }
    byte[] secret = new byte[secretLength];
    mySecureRandom.nextBytes(secret);
    SecretKey key = new SecretKeySpec(secret, algo);
    Arrays.fill(secret, (byte) 0);
    return key;
  }
}
