// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Represents a ResourceNotFoundException.
 */
public class ResourceNotFoundException extends DispatchException {

  /**
   * Constructs a ResourceNotFoundException.
   */
  public ResourceNotFoundException() {
    super();
  }

  /**
   * Constructs a ResourceNotFoundException.
   * @param cause The cause of the exception.
   */
  public ResourceNotFoundException(String cause) {
    super(cause);
  }

  /**
   * Constructs a ResourceNotFoundException.
   * @param cause The cause of the exception.
   */
  public ResourceNotFoundException(Exception cause) {
    super(cause);
  }

  @Override
  protected int getErrorCode() {
    return Const.RESOURCE_NOT_FOUND;
  }
}
