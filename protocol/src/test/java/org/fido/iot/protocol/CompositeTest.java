// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

  @Test
  void decodeHexTest() {

    byte[] arr = Composite.decodeHex("20");
    assertArrayEquals(arr, new byte[]{ 32 });

  }

  @Test
  void getAsMapTest() {

    Map<Object,Object> input = new HashMap<Object, Object>();
    input.put("K","V");
    Composite test = Composite.newMap().set("K",input);
    Map<Object,Object> res = test.getAsMap("K");
    assertTrue(res.containsKey("K") && res.containsValue("V"));

  }

  @Test
  void getAsCompositeTest() {

    Composite obj = Composite.newArray().set(1 , "V");
    Composite input = Composite.newArray().set(1, obj);
    Composite res = input.getAsComposite(1);
    assertTrue(res.get(1) == "V" );

  }
}
