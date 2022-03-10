// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

/**
 * Results in 415.
 */
public class UnsupportedMediaTypeException extends Exception {

  /**
   * Exception Constructor.
   *
   * @param value The cause of the Exception.
   */
  public UnsupportedMediaTypeException(String value) {
    super(value);
  }
}
