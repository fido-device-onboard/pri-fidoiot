package org.fidoalliance.fdo.protocol;

/**
 * The interface all Log providers implement.
 */
public interface LogProvider {

  /**
   * Logs an object to the centralized logger at the Info level.
   * @param log The object to log.
   */
  public void info(Object log);

  /**
   * Logs an object to the centralized logger at the debug level.
   * @param log The object to log.
   */
  public void debug(Object log);

  /**
   * Logs an object to the centralized logger at the error level.
   * @param log The object to log.
   */
  public void error(Object log);

  /**
   * Logs an object to the centralized logger at the warning level.
   * @param log The object to log.
   */
  public void warn(Object log);

  /**
   * Gets the name of the logger.
   * @return The name of the logger.
   */
  public String getName();
}
