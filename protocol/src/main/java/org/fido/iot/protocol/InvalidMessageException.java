// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Represents an InvalidMessageException.
 */
public class InvalidMessageException extends DispatchException {

  /**
   * Constructs an InvalidMessageException.
   */
  public InvalidMessageException() {
    super();
  }

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
  protected int getErrorCode() {
    return Const.INVALID_MESSAGE_ERROR;
  }
}
