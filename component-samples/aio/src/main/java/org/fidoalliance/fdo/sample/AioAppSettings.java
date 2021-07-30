// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

public class AioAppSettings {

  // the port at which Owner is listening for TO2
  public static final String AIO_PORT = "aio_port";
  public static final String AIO_HTTPS_PORT = "aio_https_port";

  // To set the default protocol scheme
  public static final String AIO_SCHEME = "aio_protocol_scheme";

  // SSL Keystore info
  public static final String SSL_KEYSTORE_PATH = "aio_ssl_keystore";
  public static final String SSL_KEYSTORE_PASSWORD = "aio_ssl_keystore-password";
  public static final String SSL_TRUSTSTORE_PATH = "aio_ssl_keystore";
  public static final String SSL_TRUSTSTORE_PASSWORD = "aio_ssl_keystore-password";

  // part of the path of exposed web APIs
  public static final String WEB_PATH = "/fdo/100/msg";

  // H2 database configuration keys and constants
  public static final String DB_URL = "aio_database_connection_url";
  public static final String DB_USER = "aio_database_username";
  public static final String DB_PWD = "aio_database_password";
  public static final String DB_PORT = "aio_database_port";
  public static final String DB_DRIVER = "aio_database_driver";
  public static final String DB_ALLOW_OTHERS = "aio_database_allow_others";
  public static final String DB_INIT_SQL = "aio_database_init_sql";
  public static final String DB_NEW_DEVICE_SQL = "aio_database_new_device_sql";
  public static final String DB_SESSION_CHECK_INTERVAL = "aio_session_check_interval";

  // tomcat's catalaina.home
  public static final String SERVER_PATH = "catalina_home";

  // EPID URL
  public static final String EPID_URL = "epid_online_url";
  public static final String EPID_TEST_MODE = "epid_test_mode";

  // the path and user pin for keystore that contains the owner keys
  public static final String OWNER_KEYSTORE = "owner_keystore";
  public static final String OWNER_KEYSTORE_PWD = "owner_keystore_password";
  public static final String OWNER_KEYSTORE_TYPE = "owner_keystore_type";
  public static final String OWNER_TRANSFER_KEYS = "owner_transfer_keys";
  public static final String OWNER_REPLACEMENT_KEYS = "owner_replacement_keys";

  // the path and user pin for keystore that contains the manufacturer certificate
  public static final String MANUFACTURER_KEYSTORE = "manufacturer_keystore";
  public static final String MANUFACTURER_KEYSTORE_TYPE = "manufacturer_keystore_type";
  public static final String MANUFACTURER_KEYSTORE_PWD = "manufacturer_keystore_password";

  //web server api authentication
  public static final String AUTH_METHOD = "DIGEST";
  public static final String AUTH_ROLE = "api";
  public static final String API_USER = "aio_api_user";
  public static final String API_PWD = "aio_api_password";

  // ondie settings
  public static final String ONDIE_CACHEDIR = "ondie_cache";
  public static final String ONDIE_AUTOUPDATE = "ondie_autoupdate";
  public static final String ONDIE_ZIP_ARTIFACT = "ondie_zip_artifact";
  public static final String ONDIE_CHECK_REVOCATIONS = "ondie_check_revocations";

  //servlet api config
  public static final String DOWNLOADS_PATH = "aio_downloads_path";
  public static final String AUTO_INJECT_BLOB = "aio_auto_inject_blob";
  public static final String TO0_RV_BLOB = "owner_to0_rv_blob";
}
