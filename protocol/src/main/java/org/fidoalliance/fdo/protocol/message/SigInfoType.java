// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum SigInfoType {
  SECP256R1(-7),
  SECP384R1(-35),
  RSA2048(-257),
  RSA3072(-258),
  EPID10(90),
  EPID11(91);

  private final int id;

  SigInfoType(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static SigInfoType fromNumber(Number n) {
    int i = n.intValue();

    for (SigInfoType e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(SigInfoType.class.getName() + ":" + i);

  }


  @JsonValue
  public int toInteger() {
    return id;
  }


}
