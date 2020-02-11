// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Writer;

/**
 * The interface for objects which can encode an SDO object.
 *
 * @param <T> the type of the SDO object being encoded.
 */
@FunctionalInterface
interface ProtocolEncoder<T> {
  void encode(final Writer out, final T val) throws IOException;
}
