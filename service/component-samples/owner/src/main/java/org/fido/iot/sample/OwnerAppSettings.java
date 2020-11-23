// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

/**
 * Contains the configuration property keys and constants that are used by the sample application
 * specifically.
 */
public class OwnerAppSettings {

  // the port at which Owner is listening for TO2
  public static final String TO2_PORT = "owner_to2_port";

  // part of the path of exposed web APIs
  public static final String WEB_PATH = "/fido/100/msg";

  // H2 database configuration keys and constants
  public static final String DB_URL = "owner_database_connection_url";
  public static final String DB_USER = "owner_database_username";
  public static final String DB_PWD = "owner_database_password";
  public static final String DB_PORT = "owner_database_port";
  public static final String H2_DRIVER = "org.h2.Driver";

  // tomcat's catalaina.home
  public static final String SERVER_PATH = "catalina_home";
  
  // EPID URL
  public static final String EPID_URL = "epid_online_url";

  // the user pin for HSM keystore that contains the owner keys
  public static final String OWNER_KEYSTORE_PWD = "owner_keystore_password";

  // the owner keystore type
  public static final String OWNER_KEYSTORE_TYPE = "PKCS11";

  public static final String TO0_RV_BLOB = "owner_to0_rv_blob";

  public static final String TO0_SCHEDULING_ENABLED = "owner_to0_scheduling_enabled";

  public static final String TO0_SCHEDULING_INTREVAL = "owner_to0_scheduling_interval";
}
