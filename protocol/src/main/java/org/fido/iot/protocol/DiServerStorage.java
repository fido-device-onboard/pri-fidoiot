// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Device Initialization server storage.
 */
public interface DiServerStorage extends StorageEvents {

  /**
   * Creates an new ownership voucher.
   *
   * @param mfgInfo The manufacturing information sent by the client.
   * @return New ownership voucher.
   */
  Composite createVoucher(Object mfgInfo);

  /**
   * Gets the stored ownership voucher.
   *
   * @return The existing ownership voucher.
   */
  Composite getVoucher();

  /**
   * Stores an ownership voucher.
   *
   * @param voucher The ownership voucher to store.
   */
  void storeVoucher(Composite voucher);
}
