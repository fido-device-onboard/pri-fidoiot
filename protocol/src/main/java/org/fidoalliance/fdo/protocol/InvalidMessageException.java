// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.message.ErrorCode;

/**
 * Represents an InvalidMessageException.
 */
public class InvalidMessageException extends DispatchException {

  /**
   * Constructs an InvalidMessageException.
   *
   * @param cause The cause of the exception.
   */
  public InvalidMessageException(Exception cause) {
    super(cause);
  }

  /**
   * Constructs an InvalidMessageException.
   *
   * @param cause The cause of the exception.
   */
  public InvalidMessageException(String cause) {
    super(cause);
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_MESSAGE_ERROR;
  }
}
