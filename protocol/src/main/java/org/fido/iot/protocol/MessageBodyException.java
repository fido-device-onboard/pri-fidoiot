// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Represents a MessageBodyException.
 */
public class MessageBodyException extends DispatchException {

  /**
   * Constructs an MessageBodyException.
   */
  public MessageBodyException() {
    super();
  }

  /**
   * Constructs an MessageBodyException.
   *
   * @param cause The cause of the exception.
   */
  public MessageBodyException(Exception cause) {
    super(cause);
  }

  @Override
  protected int getErrorCode() {
    return Const.MESSAGE_BODY_ERROR;
  }
}
