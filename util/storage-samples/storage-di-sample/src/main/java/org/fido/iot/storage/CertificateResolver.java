// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.security.cert.Certificate;
import org.fido.iot.protocol.CloseableKey;

/**
 * Resolves certificate chains and keys.
 */
public interface CertificateResolver {

  /**
   * Gets the private key of the certificate.
   *
   * @param cert The certificate to get the private key of.
   * @return A closable Private Key resource.
   */
  CloseableKey getPrivateKey(Certificate cert);

  /**
   * Get a certificate chain compatible with a public key type.
   *
   * @param publicKeyType The type of public key.
   * @return The certificate chain compatible with the key type.
   */
  Certificate[] getCertChain(int publicKeyType);
}
