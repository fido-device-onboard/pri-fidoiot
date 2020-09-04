// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.util.concurrent.TimeUnit;

/**
 * A Runtime Exception thrown while dispatching a message.
 */
public class DispatchException extends RuntimeException {

  /**
   * Constructs a DispatchException.
   */
  public DispatchException() {
    super();
  }

  /**
   * Constructs a DispatchException.
   *
   * @param message The cause of the exception.
   */
  public DispatchException(String message) {
    super(message);
  }

  /**
   * Constructs a DispatchException.
   *
   * @param cause The cause of the exception.
   */
  public DispatchException(Exception cause) {
    super(cause);
  }

  /**
   * Constructs a dispatch exception from a error request.
   *
   * @param request The error request.
   * @return The DispatchException.
   */
  public static DispatchException fromRequest(Composite request) {

    return null;
  }

  /**
   * Gets a Composite representing the Error.
   *
   * @param request The dispatched request.
   * @return A specification Error.
   */
  public Composite getError(Composite request) {
    //max 9007199254740991
    //max long 9223372036854775807
    Composite body = Composite.newArray()
        .set(Const.EM_ERROR_CODE, getErrorCode())
        .set(Const.EM_PREV_MSG_ID, request.getAsNumber(Const.SM_MSG_ID))
        .set(Const.EM_ERROR_STR, "")
        .set(Const.EM_ERROR_TS, getTimestamp())
        .set(Const.EM_ERROR_UUID, Const.DEFAULT);

    return Composite.newArray()
        .set(Const.SM_LENGTH, Const.DEFAULT)
        .set(Const.SM_MSG_ID, Const.ERROR)
        .set(Const.SM_PROTOCOL_VERSION, request.getAsNumber(Const.SM_PROTOCOL_VERSION))
        .set(Const.SM_PROTOCOL_INFO, Composite.newMap())
        .set(Const.SM_BODY, body);
  }

  protected long getTimestamp() {
    return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
  }

  protected int getErrorCode() {
    return Const.INTERNAL_SERVER_ERROR;
  }
}
