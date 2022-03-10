// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

/**
 * Results in 401 Error.
 */
public class NotFoundException extends Exception {

  /**
   * Exception Constructor.
   * @param message The cause of the Exception.
   */
  public NotFoundException(String message) {
    super(message);
  }
}
