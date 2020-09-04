// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.cbor;

/**
 * CBOR 'Additional Information' values, as described in RFC7049.
 */
abstract class AdditionalInfo {
  static final long MAXINT = 23;
  static final byte LENGTH_ONE = 24;
  static final byte LENGTH_TWO = 25;
  static final byte LENGTH_FOUR = 26;
  static final byte LENGTH_EIGHT = 27;
  static final byte LENGTH_INDEFINITE = 31;
  static final byte FALSE = 20;
  static final byte TRUE = 21;
  static final byte NULL = 22;
  static final byte UNDEFINED_VALUE = 23;
  static final byte BREAK = 31;
}
