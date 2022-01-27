package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.DispatchException;
import org.fidoalliance.fdo.protocol.message.ErrorCode;

public class InvalidOwnershipVoucherException extends DispatchException {


  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INVALID_OWNERSHIP_VOUCHER;
  }
}
