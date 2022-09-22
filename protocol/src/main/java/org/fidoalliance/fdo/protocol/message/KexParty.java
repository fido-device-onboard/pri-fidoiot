// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.fidoalliance.fdo.protocol.InvalidMessageException;

public enum KexParty {
  A(0),
  B(1);

  private final int id;

  KexParty(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static KexParty fromNumber(Number n) {
    int i = n.intValue();

    for (KexParty e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new RuntimeException(new InvalidMessageException(KexParty.class.getName() + ":" + i));
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}
