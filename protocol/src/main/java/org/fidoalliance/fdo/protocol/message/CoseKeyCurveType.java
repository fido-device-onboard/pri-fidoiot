// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

/**
 * Cose Key Curve Types.
 * <p>From RFC8151 13.1</p>
 */
public enum CoseKeyCurveType {
  P256EC2(1),
  P384EC2(2);

  private final int id;

  CoseKeyCurveType(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static CoseKeyCurveType fromNumber(Number n) {
    int i = n.intValue();

    for (CoseKeyCurveType e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(CoseKeyCurveType.class.getName() + ":" + i);

  }

  /**
   * Converts a String to the Type.
   * @param value The string value representing the type.
   * @return The Type represented by the String.
   */
  @JsonCreator
  public static CoseKeyCurveType fromString(String value) {

    switch (value) {
      case "P-256":
        return CoseKeyCurveType.P256EC2;
      case "P-384":
        return CoseKeyCurveType.P384EC2;
      default:
        break;
    }

    throw new NoSuchElementException(CoseKeyCurveType.class.getName() + ":" + value);

  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}

