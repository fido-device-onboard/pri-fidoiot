// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

/**
 * Results in 500 internal error.
 */
public class InternalServerErrorException extends Exception {

  /**
   * Exception Constructor.
   * @param cause The cause of the exception.
   */
  public InternalServerErrorException(Exception cause) {
    super(cause);
  }
}
