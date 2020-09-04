// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Represents an InvalidOwnershipVoucherException.
 */
public class InvalidOwnershipVoucherException extends DispatchException {

  /**
   * Constructs an InvalidOwnershipVoucherException.
   */
  public InvalidOwnershipVoucherException() {
    super();
  }

  /**
   * Constructs an InvalidOwnershipVoucherException.
   * @param cause The cause of the exception.
   */
  public InvalidOwnershipVoucherException(String cause) {
    super(cause);
  }

  /**
   * Constructs an InvalidOwnershipVoucherException.
   * @param cause The cause of the exception.
   */
  public InvalidOwnershipVoucherException(Exception cause) {
    super(cause);
  }

  @Override
  protected int getErrorCode() {
    return Const.INVALID_OWNERSHIP_VOUCHER;
  }
}
