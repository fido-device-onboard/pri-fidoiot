// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.util.UUID;

/**
 * An ownership voucher.
 *
 * <p>This is a marker interface, to group and provide type safety for all voucher implementations.
 */
public interface OwnershipVoucher extends Serializable {

  /**
   * Return this voucher's UUID (PM.OwnershipVoucher.oh.g)
   *
   * @return This voucher's UUID
   */
  UUID getUuid();
}
