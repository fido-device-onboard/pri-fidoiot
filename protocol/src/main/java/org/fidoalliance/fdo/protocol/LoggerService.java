// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

/**
 * Provides centralized infrastructure for logging.
 */
public class LoggerService {

  private final LogProvider provider;

  public LoggerService(Class<?> clazz) {
    LogProviderFactory factory = Config.getWorker(LogProviderFactory.class);
    provider = factory.apply(clazz);
  }

  public void info(Object log) {
    provider.info(log);
  }

  public void debug(Object log) {
    provider.debug(log);
  }

  public void error(Object log) {
    provider.error(log);
  }

  public void warn(Object log) {
    provider.warn(log);
  }

}
