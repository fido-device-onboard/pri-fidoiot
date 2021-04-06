// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.ondie.OnDieService;

/**
 * To0 Server Storage Interface.
 */
public interface To0ServerStorage extends StorageEvents {

  /**
   * Gets the nonceTo0Sign value from storage.
   *
   * @return
   */
  byte[] getNonceTo0Sign();

  /**
   * Sets the nonceTo0Sign value to store.
   *
   * @param nonceTo0Sign The nonce value to store.
   */
  void setNonceTo0Sign(byte[] nonceTo0Sign);

  /**
   * Stores the redirect blob.
   *
   * @param voucher       The ownership voucher associated with the blob.
   * @param requestedWait The requested wait time in seconds.
   * @param signedBlob    The signed blob to store.
   * @return The response wait time in seconds.
   */
  long storeRedirectBlob(Composite voucher, long requestedWait, byte[] signedBlob);

}
