// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Resolves a public key to a private key.
 */
public interface KeyResolver {

  /**
   * Gets the private key.
   *
   * @param key the public key to use as a query for the private key.
   * @return The private key.
   */
  PrivateKey getKey(PublicKey key);
}
