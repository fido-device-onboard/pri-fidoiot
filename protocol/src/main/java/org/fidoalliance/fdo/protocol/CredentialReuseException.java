package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.DispatchException;
import org.fidoalliance.fdo.protocol.message.ErrorCode;

public class CredentialReuseException extends DispatchException {

  public CredentialReuseException() {
    super("Credential reuse not supported");
  }
  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.CRED_REUSE_ERROR;
  }


}
