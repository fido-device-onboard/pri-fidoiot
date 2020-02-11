// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * Perishable objects are only valid for a limited time.
 */
@FunctionalInterface
public interface Perishable {

  /**
   * return true if this object has expired.
   */
  boolean isExpired();
}
