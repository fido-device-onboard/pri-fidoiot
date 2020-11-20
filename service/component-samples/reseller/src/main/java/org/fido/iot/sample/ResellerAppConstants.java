// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

public class ResellerAppConstants {
  // the port at which Owner is listening for TO2
  public static final String API_PORT = "reseller_api_port";

  // H2 database configuration keys and constants
  public static final String DB_URL = "reseller_database_connection_url";
  public static final String DB_USER = "reseller_database_username";
  public static final String DB_PWD = "reseller_database_password";
  public static final String DB_PORT = "reseller_database_port";
  public static final String DB_DRIVER = "reseller_database_driver";
  public static final String API_USER = "reseller_api_user";
  public static final String API_PWD = "reseller_api_password";

  // tomcat's catalaina.home
  public static final String SERVER_PATH = "reseller_home";

  // the owner keystore type
  public static final String KEYSTORE_TYPE = "reseller_keystore_type";
  // the user pin for HSM keystore that contains the owner keys
  public static final String KEYSTORE_PWD = "reseller_keystore_password";
  public static final String KEYSTORE_PATH = "";

  public static final String USER_DIR = "user.dir";
}
