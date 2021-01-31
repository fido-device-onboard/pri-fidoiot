// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

public class ManufacturerAppSettings {

  public static final String DI_PORT = "manufacturer_di_port";

  // part of the path of exposed web APIs
  public static final String WEB_PATH = "/fido/100/msg";
  
  // H2 database configuration keys and constants
  public static final String DB_URL = "manufacturer_database_connection_url";
  public static final String DB_USER = "manufacturer_database_username";
  public static final String DB_PWD = "manufacturer_database_password";
  public static final String DB_PORT = "manufacturer_database_port";
  public static final String H2_DRIVER = "org.h2.Driver";

  // tomcat's catalaina.home
  public static final String SERVER_PATH = "catalina_home";

  // the user pin for HSM keystore that contains the owner keys
  public static final String MFG_KEYSTORE_PWD = "manufacturer_keystore_password";

  // the owner keystore type
  public static final String MFG_KEYSTORE_TYPE = "PKCS11";

  public static final String AUTH_METHOD = "DIGEST";
  public static final String AUTH_ROLE = "api";
  public static final String API_USER = "manufacturer_api_user";
  public static final String API_PWD = "manufacturer_api_password";

}
