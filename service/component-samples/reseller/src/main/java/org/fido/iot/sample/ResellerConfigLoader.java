// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.nio.file.Path;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * Utility class to load properties/configuration from system, environment and configuration file.
 */
public class ResellerConfigLoader {

  private static final String configurationFile = "application.properties";
  private static EnvironmentConfiguration environmentConfiguration = new EnvironmentConfiguration();
  private static SystemConfiguration systemConfiguration = new SystemConfiguration();
  private static FileBasedConfiguration fileBasedConfiguration = null;
  private static PropertiesConfiguration defaultConfiguration = null;

  /**
   * Load the given configuration value.
   *
   * @param property The configuration value to load.
   * @return
   */
  public static String loadConfig(String property) {

    if (null == fileBasedConfiguration) {
      FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
          new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
              .configure(new Parameters().properties().setFileName(configurationFile));
      try {
        fileBasedConfiguration = builder.getConfiguration();
      } catch (ConfigurationException e) {
        System.out.println("Application might not be using config file");
        // ignore the error since the application might not be using config file.
        // log when logging is enabled in the application.
      }
    }
    if (null == defaultConfiguration) {
      defaultConfiguration = new PropertiesConfiguration();
      defaultConfiguration.addProperty(ResellerAppConstants.API_PORT, "8070");
      defaultConfiguration.addProperty(ResellerAppConstants.DB_DRIVER, "org.h2.Driver");
      defaultConfiguration.addProperty(ResellerAppConstants.SERVER_PATH,
          System.getProperty(ResellerAppConstants.USER_DIR));
      defaultConfiguration.addProperty(ResellerAppConstants.DB_USER, "sa");
      defaultConfiguration.addProperty(ResellerAppConstants.DB_PWD, "");
      defaultConfiguration.addProperty(ResellerAppConstants.DB_PORT, "8071");
      defaultConfiguration.addProperty(ResellerAppConstants.KEYSTORE_TYPE, "PKCS11");
      defaultConfiguration.addProperty(ResellerAppConstants.KEYSTORE_PATH, "");
      defaultConfiguration.addProperty(ResellerAppConstants.KEYSTORE_PWD, "RsrKs@3er");
      defaultConfiguration.addProperty(ResellerAppConstants.API_USER, "admin");
      defaultConfiguration.addProperty(ResellerAppConstants.API_PWD, "test");

      final String url = "jdbc:h2:tcp://localhost:8071/"
          + Path.of(System.getProperty(ResellerAppConstants.USER_DIR),
          "reseller").toString();

      defaultConfiguration.addProperty(ResellerAppConstants.DB_URL, url);
    }
    if (systemConfiguration.containsKey(property)) {
      return systemConfiguration.interpolatedConfiguration().getString(property);
    } else if (environmentConfiguration.containsKey(property)) {
      return environmentConfiguration.getString(property);
    } else if (null != fileBasedConfiguration
        && fileBasedConfiguration.containsKey(property)) {
      return fileBasedConfiguration.getString(property);
    } else if (defaultConfiguration.containsKey(property)) {
      return defaultConfiguration.getProperty(property).toString();
    }
    throw new RuntimeException();
  }
}
