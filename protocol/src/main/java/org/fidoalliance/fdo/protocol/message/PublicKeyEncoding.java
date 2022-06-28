// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;


public enum PublicKeyEncoding {
  CRYPTO(0),
  X509(1),
  COSEX5CHAIN(2),
  COSEKEY(3);

  private final int id;

  PublicKeyEncoding(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static PublicKeyEncoding fromNumber(Number n) {
    int i = n.intValue();

    for (PublicKeyEncoding e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(PublicKeyEncoding.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}

