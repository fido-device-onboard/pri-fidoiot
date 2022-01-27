package org.fidoalliance.fdo.protocol;

import java.io.IOException;

/**
 * Represents an InvalidMessageException.
 */
public class InvalidMessageException extends IOException {

  /**
   * Constructs an InvalidMessageException.
   *
   * @param cause The cause of the exception.
   */
  public InvalidMessageException(Exception cause) {
    super(cause);
  }

  /**
   * Constructs an InvalidMessageException.
   *
   * @param cause The cause of the exception.
   */
  public InvalidMessageException(String cause) {
    super(cause);
  }
}
