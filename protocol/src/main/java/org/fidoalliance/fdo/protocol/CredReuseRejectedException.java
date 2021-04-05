package org.fidoalliance.fdo.protocol;

/**
 * Represents credentials reuse rejected exception.
 */
public class CredReuseRejectedException extends DispatchException {

  /**
   * Constructs a cred reuse rejected exception.
   */
  public CredReuseRejectedException() {
    super();
  }

  /**
   * Constructs a cred reuse rejected exception.
   *
   * @param cause The cause of the exception.
   */
  public CredReuseRejectedException(Exception cause) {
    super(cause);
  }

  @Override
  protected int getErrorCode() {
    return Const.CRED_REUSE_ERROR;
  }
}
