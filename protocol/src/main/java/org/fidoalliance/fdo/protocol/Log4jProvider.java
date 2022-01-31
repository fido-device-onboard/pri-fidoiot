package org.fidoalliance.fdo.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Log4j Provider.
 */
public class Log4jProvider implements LogProvider {

  private final Logger logger;


  /**
   * Constructs a Log4jProvider
   * @param clazz
   */
  public Log4jProvider(Class<?> clazz) {
    logger = LoggerFactory.getLogger(clazz);
  }

  @Override
  public void info(Object log) {
    logger.info(log.toString());
  }

  @Override
  public void debug(Object log) {
    logger.info(log.toString());
  }

  @Override
  public void error(Object log) {
    logger.info(log.toString());
  }

  @Override
  public void warn(Object log) {
    logger.info(log.toString());
  }

  @Override
  public String getName() {
    return logger.getName();
  }
}
