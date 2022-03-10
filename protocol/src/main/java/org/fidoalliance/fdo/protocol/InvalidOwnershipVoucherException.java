// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.message.ErrorCode;

public class InvalidOwnershipVoucherException extends DispatchException {

  /**
   * Constructs an invalid guid exception.
   *
   * @param cause The cause of the exception.
   */
  public InvalidOwnershipVoucherException(String cause) {
    super(cause);
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_OWNERSHIP_VOUCHER;
  }
}
