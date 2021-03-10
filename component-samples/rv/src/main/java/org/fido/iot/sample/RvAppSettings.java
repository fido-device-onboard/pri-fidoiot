// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

public class RvAppSettings {

  // the port at which Rv is listening for TO0 & TO1
  public static final String TO0_TO1_PORT = "rv_port";
  public static final String RV_HTTPS_PORT = "rv_https_port";

  // To set the default protocol scheme
  public static final String RV_SCHEME = "rv_protocol_scheme";

  // SSL Keystore info
  public static final String SSL_KEYSTORE_PATH = "rv_ssl_keystore";
  public static final String SSL_KEYSTORE_PASSWORD = "rv_ssl_keystore-password";

  // part of the path of exposed web APIs
  public static final String WEB_PATH = "/fdo/100/msg";

  // H2 database configuration keys and constants
  public static final String DB_URL = "rv_database_connection_url";
  public static final String DB_USER = "rv_database_username";
  public static final String DB_PWD = "rv_database_password";
  public static final String DB_PORT = "rv_database_port";
  public static final String H2_DRIVER = "org.h2.Driver";

  // tomcat's catalaina.home
  public static final String SERVER_PATH = "catalina_home";

  // EPID URL
  public static final String EPID_URL = "epid_online_url";
  public static final String EPID_TEST_MODE = "epid_test_mode";

}
