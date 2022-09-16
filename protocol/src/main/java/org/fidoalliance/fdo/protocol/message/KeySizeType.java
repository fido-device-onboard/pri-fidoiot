// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum KeySizeType {
  SIZE_2048(2048),
  SIZE_3072(3072),
  SIZE_256(256),
  SIZE_384(384);


  private final int id;

  KeySizeType(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static KeySizeType fromNumber(Number n) {
    int i = n.intValue();

    for (KeySizeType e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(KeySizeType.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}
