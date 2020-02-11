// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;

/**
 * Provides message-digest services for SDO.
 */
@FunctionalInterface
interface MacService {

  /**
   * Returns the MAC of the input.
   *
   * @param in an array of {@link ByteBuffer} inputs.
   * @return the completed MAC.
   */
  HashMac macOf(final ByteBuffer... in);
}
