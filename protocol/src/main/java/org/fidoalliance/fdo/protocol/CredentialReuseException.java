package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.DispatchException;
import org.fidoalliance.fdo.protocol.message.ErrorCode;

public class CredentialReuseException extends DispatchException {

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.CRED_REUSE_ERROR;
  }

}
