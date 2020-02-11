// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;

/**
 * Provides message-digest services for SDO.
 */
interface DigestService {

  /**
   * Returns the digest of the input.
   *
   * @param in an array of {@link ByteBuffer} inputs.
   * @return the completed digest.
   */
  HashDigest digestOf(final ByteBuffer... in);
}
