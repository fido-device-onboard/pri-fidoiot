package org.fidoalliance.fdo.protocol;

public interface HttpServer extends Runnable {
  String getHttpPort();
  String getHttpsPort();
}
