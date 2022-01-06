package org.fidoalliance.fdo.protocol.api;

public class BadRequestException extends Exception {

  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(Exception cause) {
    super(cause);
  }

  public BadRequestException(String message, Exception cause) {
    super(message,cause);
  }
}
