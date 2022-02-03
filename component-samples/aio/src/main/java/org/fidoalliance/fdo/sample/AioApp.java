package org.fidoalliance.fdo.sample;


import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.db.HibernateUtil;
import org.fidoalliance.fdo.protocol.HttpServer;


public class AioApp {




  public static void main(String args[]) {
    HibernateUtil.getSessionFactory();

    Config.getWorker(HttpServer.class).run();
  }
}
