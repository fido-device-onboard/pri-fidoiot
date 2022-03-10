// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.db.HibernateUtil;


public class AioApp {

  static LoggerService logger = new LoggerService(AioApp.class);

  /**
   * Main entry.
   * @param args Commandline arguments.
   */
  public static void main(String[] args) {
    try {
      HibernateUtil.getSessionFactory();
      Config.getWorker(HttpServer.class).run();
    } catch (Throwable throwable) {
      HibernateUtil.shutdown();
      logger.error(throwable.getMessage());
    }
  }
}
