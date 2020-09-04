// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Device Initialization client storage.
 */
public interface DiClientStorage extends StorageEvents, DeviceCredentials {

  /**
   * Gets the Manufacturing Information for the device.
   *
   * @return The Manufacturing information.
   */
  Object getDeviceMfgInfo();
}
