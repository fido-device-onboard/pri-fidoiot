// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.cbor;

/**
 * CBOR common tags, as described in RFC7049.
 */
abstract class Tag {
  static final int POSITIVE_BIGNUM = 2;
  static final int NEGATIVE_BIGNUM = 3;
}
