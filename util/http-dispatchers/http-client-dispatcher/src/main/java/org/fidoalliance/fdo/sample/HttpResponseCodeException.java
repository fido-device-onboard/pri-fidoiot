package org.fidoalliance.fdo.sample;

public class HttpResponseCodeException extends Exception {

  private final int myCode;

  HttpResponseCodeException(int code) {
    super();
    myCode = code;
  }

  HttpResponseCodeException(int code, String message) {
    super(message);
    myCode = code;
  }

  public int getCode() {
    return myCode;
  }
}
