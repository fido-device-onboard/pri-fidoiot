// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;


public enum RendezvousVariable {
  DEV_ONLY(0),//
  OWNER_ONLY(1),//
  IP_ADDRESS(2),//
  DEV_PORT(3),//
  OWNER_PORT(4),//
  DNS(5),//
  SV_CERT_HASH(6),//
  CL_CERT_HASH(7),//
  USER_INPUT(8),//
  WIFI_SSID(9),//
  WIFI_PW(10),//
  MEDIUM(11),//
  PROTOCOL(12),//
  DELAYSEC(13),//
  BYPASS(14),
  EXT_RV(15);

  private final int id;

  RendezvousVariable(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static RendezvousVariable fromNumber(Number n) {
    int i = n.intValue();

    for (RendezvousVariable e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(
        RendezvousVariable.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}
