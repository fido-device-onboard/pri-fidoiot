// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * A Runtime exception thrown by the CryptoService.
 */
public class CryptoServiceException extends RuntimeException {

  /**
   * Constructs a CryptoService exception.
   *
   * @param cause The cause of the exception.
   */
  public CryptoServiceException(Exception cause) {
    super(cause);
  }
}
