// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.loggingutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a wrapper class for Slf4j Logger and supporting centralized infrastructure for logging.
 */
public class LoggerService {

  public static Logger logger;

  public LoggerService(Class<?> className) {
    logger = LoggerFactory.getLogger(className);
  }

  public void info(Object log) {
    logger.info(log.toString());
  }

  public void debug(Object log) {
    logger.debug(log.toString());
  }

  public void error(Object log) {
    logger.error(log.toString());
  }

  public void warn(Object log) {
    logger.warn(log.toString());
  }

}
