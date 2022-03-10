// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

/**
 * Results in 400 back request.
 */
public class BadRequestException extends Exception {

  /**
   * Exception Constructor.
   *
   * @param message The cause of the exception.
   */
  public BadRequestException(String message) {
    super(message);
  }

  /**
   * Exception Constructor.
   *
   * @param cause The cause of the exception.
   */
  public BadRequestException(Exception cause) {
    super(cause);
  }


  /**
   * Exception Constructor.
   *
   * @param message The message cause of the exception.
   * @param cause The exception cause of the exception.
   */
  public BadRequestException(String message, Exception cause) {
    super(message, cause);
  }
}
