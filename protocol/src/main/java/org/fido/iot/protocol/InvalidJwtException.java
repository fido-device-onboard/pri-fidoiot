// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Represents an InvalidJwtException.
 */
public class InvalidJwtException extends DispatchException {

  /**
   * Constructs an InvalidJwtException.
   */
  public InvalidJwtException() {
    super();
  }

  /**
   * Constructs an InvalidJwtException.
   *
   * @param cause The cause of the exception.
   */
  public InvalidJwtException(String cause) {
    super(cause);
  }

  /**
   * Constructs an InvalidJwtException.
   *
   * @param cause The cause of the exception.
   */
  public InvalidJwtException(Exception cause) {
    super(cause);
  }

  @Override
  protected int getErrorCode() {
    return Const.INVALID_JWT_TOKEN;
  }

}

