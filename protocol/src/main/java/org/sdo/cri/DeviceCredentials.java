// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.UUID;

/**
 * A device (ownership) credential.
 *
 * <p>This is a marker interface, to group and provide type safety for all voucher implementations.
 */
public interface DeviceCredentials {

  /**
   * Return this credential's UUID (PM.DeviceCredentials.O.g)
   *
   * @return This voucher's UUID
   */
  UUID getUuid();
}
