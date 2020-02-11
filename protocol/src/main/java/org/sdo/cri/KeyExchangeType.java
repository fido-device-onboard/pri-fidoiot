// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * SDO key-exchange algorithms.
 *
 * @see "SDO Protocol Specification, 1.12, 2.5.5: Key Exchange in the TO2 Protocol"
 */
enum KeyExchangeType {
  ASYMKEX, // ASYMKEX 2K
  DHKEXid14,
  DHKEXid15,
  ECDH, // ECDH256
  ECDH384,
  ASYMKEX3072
}
