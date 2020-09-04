// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Represents an InvalidOwnerSignBodyException.
 */
public class InvalidOwnerSignBodyException extends DispatchException {

  /**
   * Constructs an InvalidOwnerSignBodyException.
   */
  public InvalidOwnerSignBodyException() {
    super();
  }

  @Override
  protected int getErrorCode() {
    return Const.INVALID_OWNER_SIGN_BODY;
  }
}
