// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.cbor;

/**
 * CBOR 'Major Types', as described in RFC7049.
 */
abstract class MajorType {
  static final byte UNSIGNED_INT = 0;
  static final byte NEGATIVE_INT = 1;
  static final byte BYTE_STRING = 2;
  static final byte TEXT_STRING = 3;
  static final byte ARRAY = 4;
  static final byte MAP = 5;
  static final byte TAG = 6;
  static final byte SIMPLE = 7;
}
