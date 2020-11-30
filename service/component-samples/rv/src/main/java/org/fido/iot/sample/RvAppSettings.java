package org.fido.iot.sample;

public class RvAppSettings {

  // the port at which Rv is listening for TO0 & TO1
  public static final String TO0_TO1_PORT = "rv_port";

  // part of the path of exposed web APIs
  public static final String WEB_PATH = "/fido/100/msg";

  // H2 database configuration keys and constants
  public static final String DB_URL = "rv_database_connection_url";
  public static final String DB_USER = "rv_database_username";
  public static final String DB_PWD = "rv_database_password";
  public static final String DB_PORT = "rv_database_port";
  public static final String DB_TCP_SERVER = "db.tcpServer";
  public static final String H2_DRIVER = "org.h2.Driver";
  public static final String H2_ALLOW_REMOTE_CONNECTION = "webAllowOthers";

  // tomcat's catalaina.home
  public static final String SERVER_PATH = "catalina_home";

  // EPID URL
  public static final String EPID_URL = "epid_online_url";
}
