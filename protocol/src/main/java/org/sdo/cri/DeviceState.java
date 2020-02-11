// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.NoSuchElementException;

/**
 * SDO device states.
 *
 * @see "SDO Architecture Specification, 1.0, 4.2.2: Device States"
 */
enum DeviceState {
  PD(0), // Permanently Disabled
  PC(1), // Pre-Configured
  D(2), // Disabled
  READY1(3), // Initial Transfer Reader
  D1(4), // Initial Transfer Disabled
  IDLE(5), // Idle
  READYN(6), // Transfer Ready
  DN(7), // Transfer Disabled
  ERROR(255);

  private int id;

  private DeviceState(int id) {
    this.id = id;
  }

  /**
   * Convert a Number into a DeviceState.
   */
  public static DeviceState fromNumber(Number n) {
    final int i = n.intValue();

    for (DeviceState s : DeviceState.values()) {

      if (s.getId() == i) {
        return s;
      }
    }

    throw new NoSuchElementException(n.toString());
  }

  public int toInteger() {
    return getId();
  }

  private int getId() {
    return id;
  }
}
