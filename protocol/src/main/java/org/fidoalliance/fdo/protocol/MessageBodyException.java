// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;

/**
 * Represents an MessageBodyException.
 */
public class MessageBodyException extends IOException {

  /**
   * Constructs an MessageBodyException.
   *
   * @param cause The cause of the exception.
   */
  public MessageBodyException(Exception cause) {
    super(cause);
  }

  /**
   * Constructs an MessageBodyException.
   *
   * @param message The cause of the exception.
   */
  public MessageBodyException(String message) {
    super(message);
  }
}
