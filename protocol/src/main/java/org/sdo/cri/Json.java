// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class Json {

  public static final Character BEGIN_ARRAY = '[';
  public static final Character BEGIN_OBJECT = '{';
  public static final Character COLON = ':';
  public static final Character COMMA = ',';
  public static final Character END_ARRAY = ']';
  public static final Character END_OBJECT = '}';
  public static final Character QUOTE = '"';

  public static String asKey(String name) {
    return QUOTE + name + QUOTE + COLON;
  }
}
