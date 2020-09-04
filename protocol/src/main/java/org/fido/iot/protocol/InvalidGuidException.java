// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

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
  protected int getErrorCode() {
    return Const.INVALID_GUID;
  }
}
