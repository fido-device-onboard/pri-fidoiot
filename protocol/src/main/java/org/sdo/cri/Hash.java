// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;

/**
 * SDO type "Hash".
 *
 * @see "SDO Protocol Specification, 1.12k, 3.2: Composite Types"
 */
interface Hash<T> {

  ByteBuffer getHash();

  T getType();
}
