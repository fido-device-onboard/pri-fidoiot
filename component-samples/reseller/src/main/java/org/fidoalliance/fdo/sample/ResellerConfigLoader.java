// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.nio.file.Path;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.fidoalliance.fdo.loggingutils.LoggerService;

/**
 * Utility class to load properties/configuration from system, environment and configuration file.
 */
public class ResellerConfigLoader {

  private static final String configurationFile = "application.properties";
  private static EnvironmentConfiguration environmentConfiguration = new EnvironmentConfiguration();
  private static SystemConfiguration systemConfiguration = new SystemConfiguration();
  private static FileBasedConfiguration fileBasedConfiguration = null;
  private static PropertiesConfiguration defaultConfiguration = null;
  private static final LoggerService logger = new LoggerService(ResellerConfigLoader.class);

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
