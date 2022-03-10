// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.ErrorCode;

public abstract class DispatchException extends IOException {

  protected DispatchException() {
    super();
  }

  protected DispatchException(String message) {
    super(message);
  }

  protected DispatchException(Exception cause) {
    super(cause);
  }

  protected DispatchException(String message, Exception cause) {
    super(message, cause);
  }

  /**
   * The spec defined error code.
   *
   * @return An instance of Error Code.
   */
  public abstract ErrorCode getErrorCode();
}
