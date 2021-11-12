// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.cbor;

/**
 * CBOR common tags, as described in RFC7049.
 */
abstract class Tag {
  static final int POSITIVE_BIGNUM = 2;
  static final int NEGATIVE_BIGNUM = 3;
  static final int COSE_ENCRYPT0 = 16;
  static final int COSE_MAC0 = 17;
  static final int COSE_SIGN1 = 18;
}
