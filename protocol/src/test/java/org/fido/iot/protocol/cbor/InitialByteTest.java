// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.cbor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class InitialByteTest {

  @Test
  void testEquality() {
    InitialByte left = new InitialByte(1, 2);
    InitialByte right = new InitialByte(1, 2);

    assertEquals(left, left);
    assertEquals(left, right);
    assertNotEquals(left, new InitialByte(2, 3));
    assertNotEquals(left, left.toByte());
  }
}
