// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.message.ErrorCode;

public class InvalidJwtTokenException extends DispatchException {

  /**
   * Constructs an invalid JwtToken exception.
   *
   * @param message The message.
   */
  public InvalidJwtTokenException(String message) {
    super(message);
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_JWT_TOKEN;
  }
}
