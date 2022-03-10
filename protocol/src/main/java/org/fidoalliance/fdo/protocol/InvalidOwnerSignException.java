// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.message.ErrorCode;

public class InvalidOwnerSignException extends DispatchException {

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_OWNER_SIGN_BODY;
  }
}
