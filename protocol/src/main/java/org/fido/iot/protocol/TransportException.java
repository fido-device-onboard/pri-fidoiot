// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Signals that a error occurred when read or writing from the underling message transport.
 */
public class TransportException extends RuntimeException {

  public TransportException() {
    super();
  }

  public TransportException(Exception cause) {
    super(cause);
  }
}
