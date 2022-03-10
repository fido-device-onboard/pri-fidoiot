// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.message.ErrorCode;

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
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_IP_ADDRESS;
  }
}
