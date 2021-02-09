// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

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
public class OwnerConfigLoader {

  private static final String configurationFile = "application.properties";
  private static EnvironmentConfiguration environmentConfiguration;
  private static SystemConfiguration systemConfiguration;
  private static FileBasedConfiguration fileBasedConfiguration;

  static {
    environmentConfiguration = new EnvironmentConfiguration();
    systemConfiguration = new SystemConfiguration();
    FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
        new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
            .configure(new Parameters().properties().setFileName(configurationFile));
    try {
      fileBasedConfiguration = builder.getConfiguration();
      // configuration will be null if the configurationFile doesn't exist.
      // throwing exception for logging purposes.
      if (null == fileBasedConfiguration) {
        throw new ConfigurationException();
      }
    } catch (ConfigurationException e) {
      System.out.println("The Owner application might not be using config file");
      // ignore the error since the application might not be using config file.
      // log when logging is enabled in the application.
    }
  }

  /**
   * Load the given configuration value.
   *
   * @param property The configuration value to load.
   * @return
   */
  public static String loadConfig(String property) {
    if (systemConfiguration.containsKey(property)) {
      return systemConfiguration.interpolatedConfiguration().getString(property);
    } else if (environmentConfiguration.containsKey(property)) {
      return environmentConfiguration.getString(property);
    } else if (null != fileBasedConfiguration && fileBasedConfiguration.containsKey(property)) {
      return fileBasedConfiguration.getString(property);
    }
    System.out.println("Could not load property: " + property);
    return null;
  }
}
