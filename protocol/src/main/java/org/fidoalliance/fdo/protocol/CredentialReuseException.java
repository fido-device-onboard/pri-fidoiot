// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.message.ErrorCode;

public class CredentialReuseException extends DispatchException {

  /**
   * Exception constructor.
   */
  public CredentialReuseException() {
    super("Credential reuse not supported");
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.CRED_REUSE_ERROR;
  }


}
