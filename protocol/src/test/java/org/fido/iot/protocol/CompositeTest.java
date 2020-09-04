// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CompositeTest {

  @Test
  void Test() throws Exception {

    Composite test = Composite.newArray();
    assertTrue(test.size() == 0);

    test.set(Const.FIRST_KEY, null);
    assertTrue(test.size() == 1);
    assertTrue(test.get(Const.FIRST_KEY) == null);

  }
}
