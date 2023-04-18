// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.DatabaseServer;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.dispatch.ExceptionConsumer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Hibernate Utilities.
 */
public class HibernateUtil {

  private static final LoggerService logger = new LoggerService(HibernateUtil.class);

  private static class RootConfig {

    @JsonProperty("hibernate-properties")
    private final Map<String, String> hibernateProperties = new HashMap<>();

  }

  private static final SessionFactory sessionFactory = buildSessionFactory();

  private static SessionFactory buildSessionFactory() {
    try {

      final File file = Path.of(Config.getPath(), "hibernate.cfg.xml").toFile();
      final Configuration cfg = new Configuration();

      cfg.configure(file);

      final Map<String, String> map = Config.getConfig(RootConfig.class).hibernateProperties;
      for (Map.Entry<String, String> entry : map.entrySet()) {
        cfg.setProperty(entry.getKey(), Config.resolve(entry.getValue()));
      }

      final DatabaseServer server = Config.getWorker(DatabaseServer.class);
      server.start();

      final SessionFactory factory = cfg.buildSessionFactory();

      return factory;

    } catch (Throwable e) {
      try {
        Config.getWorker(ExceptionConsumer.class).accept(e);
      } catch (Exception innerException) {
        logger.error("failed log exception");
      }
      // Make sure you log the exception, as it might be swallowed
      logger.error("database session factory setup failed");
      return null;
    }
  }

  /**
   * Gets the configured Hibernate Session factory.
   *
   * @return The sessionFactory.
   */
  public static SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  /**
   * Unwraps a Blob to bytes.
   *
   * @param blob The blob to unwrap.
   * @return The bytes represented by the blob.
   * @throws IOException An error occurred.
   */
  public static byte[] unwrap(Blob blob) throws IOException {
    try {
      int length = Long.valueOf(blob.length()).intValue();
      return blob.getBytes(Long.valueOf(1), length);
    } catch (SQLException e) {
      logger.error("SQL Exception: " + e.getMessage());
      throw new IOException(e);
    }
  }




  /**
   * Shutdown hibernate pools and database.
   */
  public static void shutdown() {

    try {
      if (sessionFactory != null) {
        sessionFactory.close();
      }
    } catch (Throwable throwable) {
      logger.error(throwable.getMessage());
    }

    //sessionFactory.ge
    // Close caches and connection pools
    for (Object worker : Config.getWorkers()) {
      if (worker instanceof Closeable) {
        try {
          ((Closeable) worker).close();
        } catch (Throwable e) {
          logger.error(e.getMessage());
        }
      }
    }


  }
}