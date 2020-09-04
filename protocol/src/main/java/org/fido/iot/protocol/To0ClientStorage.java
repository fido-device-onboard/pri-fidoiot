// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * To0 Client Storage Interface.
 */
public interface To0ClientStorage extends StorageEvents {

  /**
   * Get the voucher to send for the TO0 protocol.
   *
   * @return A ownership Voucher.
   */
  Composite getVoucher();

  /**
   * Gets the redirect Rendezvous blob.
   *
   * @return A composite Rendezvous 'Blob'.;
   */
  Composite getRedirectBlob();

  /**
   * Gets the requested wait seconds to send to the Rendezvous service.
   *
   * @return The requested wait time in seconds.
   */
  long getRequestWait();

  /**
   * Sets the requested wait time received from the Rendezvous service.
   *
   * @param wait The wait time in seconds.
   */
  void setResponseWait(long wait);

  /**
   * Gets the Owners Signing key.
   *
   * @param ownerPublicKey The public key of the owner.
   * @return The Private signing key.
   */
  PrivateKey getOwnerSigningKey(PublicKey ownerPublicKey);
}
