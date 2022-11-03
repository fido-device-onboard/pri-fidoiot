// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

/**
 * Hash Algorithm types.
 */
public enum HashType {
  SHA256(-16),
  SHA384(-43),
  HMAC_SHA256(5),
  HMAC_SHA384(6);


  private final int id;

  HashType(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static HashType fromNumber(Number n) {
    int i = n.intValue();

    for (HashType e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(HashType.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}
