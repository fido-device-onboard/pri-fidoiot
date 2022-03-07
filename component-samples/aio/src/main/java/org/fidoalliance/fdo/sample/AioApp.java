package org.fidoalliance.fdo.sample;


import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.db.HibernateUtil;
import org.fidoalliance.fdo.protocol.HttpServer;

public class AioApp {

  static LoggerService logger = new LoggerService(AioApp.class);
  public static void main(String args[]) {
    try {
      HibernateUtil.getSessionFactory();
      Config.getWorker(HttpServer.class).run();
    } catch (Throwable throwable) {
      HibernateUtil.shutdown();
      logger.error(throwable.getMessage());
    }
  }
}
