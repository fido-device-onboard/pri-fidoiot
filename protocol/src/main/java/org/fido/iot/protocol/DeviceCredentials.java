// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Provides device credentials to a service.
 */
public interface DeviceCredentials {

  /**
   * Gets a Composite containing device credentials.
   *
   * @return The Composite containing device credentials.
   */
  Composite getDeviceCredentials();
}
