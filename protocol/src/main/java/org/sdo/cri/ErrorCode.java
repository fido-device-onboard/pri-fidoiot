// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.NoSuchElementException;

/**
 * Error codes defined by the secure device onboard protocol.
 */
public enum ErrorCode {
  OK(0),

  // Request tokens are not valid
  InvalidToken(1),

  // One of Ownership Proxy verification checks has failed.
  InvalidOwnershipProxy(2),

  // Verification of signature of owner message failed.
  InvalidOwnerSignBody(3),

  // IP address is invalid.
  InvalidIpAddress(4),

  // GUID is invalid.
  InvalidGuid(5),

  // Requested resources are not available on the server
  ResourceNotFound(6),

  // Message body structurally unsound: JSON parse error, or
  // valid JSON, but not mapping to expected types
  SyntaxError(100),

  // Message syntactically sound, but rejected by server
  MessageRefused(101),

  // Unexpected system error
  InternalError(500);

  private final int code;

  private ErrorCode(int code) {
    this.code = code;
  }

  /**
   * Convert an error code number to its corresponding enum.
   */
  public static ErrorCode fromNumber(final Number n) {
    int i = n.intValue();

    for (ErrorCode t : ErrorCode.values()) {
      if (t.toInteger() == i) {
        return t;
      }
    }

    throw new NoSuchElementException(n.toString());
  }

  public int toInteger() {
    return code;
  }
}
