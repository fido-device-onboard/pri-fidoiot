// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Represents an InvalidIpAddressException.
 */
public class InvalidIpAddressException extends DispatchException {

  /**
   * Constructs an InvalidIpException.
   *
   * @param cause The cause of the exception.
   */
  public InvalidIpAddressException(Exception cause) {
    super(cause);
  }

  @Override
  protected int getErrorCode() {
    return Const.INVALID_IP_ADDRESS;
  }
}
