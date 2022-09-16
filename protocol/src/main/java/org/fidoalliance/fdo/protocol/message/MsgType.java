// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;
import org.apache.commons.lang3.math.NumberUtils;
import org.fidoalliance.fdo.protocol.InvalidMessageException;

public enum MsgType {
  ERROR(255),
  DI_APP_START(10),
  DI_SET_CREDENTIALS(11),
  DI_SET_HMAC(12),
  DI_DONE(13),
  TO0_HELLO(20),
  TO0_HELLO_ACK(21),
  TO0_OWNER_SIGN(22),
  TO0_ACCEPT_OWNER(23),
  TO1_HELLO_RV(30),
  TO1_HELLO_RV_ACK(31),
  TO1_PROVE_TO_RV(32),
  TO1_RV_REDIRECT(33),
  TO2_HELLO_DEVICE(60),
  TO2_PROVE_OV_HDR(61),
  TO2_GET_OV_NEXT_ENTRY(62),
  TO2_OV_NEXT_ENTRY(63),
  TO2_PROVE_DEVICE(64),
  TO2_SETUP_DEVICE(65),
  TO2_DEVICE_SERVICE_INFO_READY(66),
  TO2_OWNER_SERVICE_INFO_READY(67),
  TO2_DEVICE_SERVICE_INFO(68),
  TO2_OWNER_SERVICE_INFO(69),
  TO2_DONE(70),
  TO2_DONE2(71);

  private final int id;

  MsgType(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static MsgType fromNumber(Number n) throws InvalidMessageException {
    int i = n.intValue();

    for (MsgType e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new InvalidMessageException(new
        NoSuchElementException(MsgType.class.getName() + ":" + i));

  }

  /**
   * Converts to type from a string value.
   * @param value The string value.
   * @return The message type from a string value.
   * @throws InvalidMessageException An error occurred.
   */
  public static MsgType fromString(String value) throws InvalidMessageException {
    if (NumberUtils.isCreatable(value)) {
      return fromNumber(Integer.valueOf(value));
    }
    throw new InvalidMessageException(new IllegalArgumentException("not a message id"));
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
