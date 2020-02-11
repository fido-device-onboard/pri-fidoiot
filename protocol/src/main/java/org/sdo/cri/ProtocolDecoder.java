// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * The interface for objects which can decode an SDO object.
 *
 * @param <T> the type of the SDO object being decoded.
 */
@FunctionalInterface
interface ProtocolDecoder<T> {
  T decode(final CharBuffer in) throws IOException;
}
