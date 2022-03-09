package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.message.ErrorCode;

public class InvalidOwnerSignException extends DispatchException {

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_OWNER_SIGN_BODY;
  }
}
