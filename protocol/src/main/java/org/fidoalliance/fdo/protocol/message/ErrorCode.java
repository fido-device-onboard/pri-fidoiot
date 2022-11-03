// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.NoSuchElementException;

public enum ErrorCode {
  INVALID_JWT_TOKEN(1),
  INVALID_OWNERSHIP_VOUCHER(2),
  INVALID_OWNER_SIGN_BODY(3),
  INVALID_IP_ADDRESS(4),
  INVALID_GUID(5),
  RESOURCE_NOT_FOUND(6),
  MESSAGE_BODY_ERROR(100),
  INVALID_MESSAGE_ERROR(101),
  CRED_REUSE_ERROR(102),
  INTERNAL_SERVER_ERROR(500);

  private final int id;

  ErrorCode(int id) {
    this.id = id;
  }

  /**
   * Converts a number to the Type.
   * @param n The number to convert from.
   * @return The Type represented by the number.
   */
  @JsonCreator
  public static ErrorCode fromNumber(Number n) {
    int i = n.intValue();

    for (ErrorCode e : values()) {

      if (e.id == i) {
        return e;
      }
    }

    throw new NoSuchElementException(ErrorCode.class.getName() + ":" + i);
  }

  @JsonValue
  public int toInteger() {
    return id;
  }
}

