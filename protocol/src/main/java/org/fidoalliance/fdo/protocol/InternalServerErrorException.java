// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;


/**
 * Represents an InternalServerErrorException.
 */
public class InternalServerErrorException extends IOException {

  /**
   * Constructs an InternalServerErrorException.
   *
   * @param cause The cause of the exception.
   */
  public InternalServerErrorException(Exception cause) {
    super(cause);
  }

  /**
   * Constructs an InternalServerErrorException.
   *
   * @param cause The cause of the exception.
   */
  public InternalServerErrorException(String cause) {
    super(cause);
  }
}
