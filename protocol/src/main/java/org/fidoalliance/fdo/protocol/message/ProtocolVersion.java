// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;
import org.fidoalliance.fdo.protocol.InvalidMessageException;

public enum ProtocolVersion {
  V100(100),
  V101(101);

  private final int id;

  ProtocolVersion(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static ProtocolVersion fromNumber(Number n) {
    int i = n.intValue();

    for (ProtocolVersion e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(ProtocolVersion.class.getName() + ":" + i);

  }

  public static ProtocolVersion current() {
    return ProtocolVersion.V101;
  }

  /**
   * Converts to type from a string value.
   * @param value The string value.
   * @return The message type from a string value.
   */
  public static ProtocolVersion fromString(String value) {

    switch (value) {
      case "100":
        return ProtocolVersion.V100;
      case "101":
        return ProtocolVersion.V101;
      default:
        break;
    }

    throw new NoSuchElementException(ProtocolVersion.class.getName() + ":" + value);

  }

  @JsonValue
  public int toInteger() {
    return id;
  }


  @Override
  public String toString() {
    return Integer.toString(id);
  }
}
