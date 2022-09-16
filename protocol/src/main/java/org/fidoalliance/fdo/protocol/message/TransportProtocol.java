// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum TransportProtocol {
  PROT_TCP(1),
  PROT_TLS(2),
  PROT_HTTP(3),
  PROT_COAP(4),
  PROT_HTTPS(5),
  PROT_COAPS(6);


  private final int id;

  TransportProtocol(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static TransportProtocol fromNumber(Number n) {
    int i = n.intValue();

    for (TransportProtocol e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(
        TransportProtocol.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}
