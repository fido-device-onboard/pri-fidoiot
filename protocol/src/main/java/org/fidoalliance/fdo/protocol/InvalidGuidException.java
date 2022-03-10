// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.message.ErrorCode;

/**
 * Represents an invalid GUID exception.
 */
public class InvalidGuidException extends DispatchException {

  /**
   * Constructs an invalid guid exception.
   *
   * @param cause The cause of the exception.
   */
  public InvalidGuidException(Exception cause) {
    super(cause);
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_GUID;
  }


}
