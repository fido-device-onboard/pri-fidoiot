// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.fidoalliance.fdo.protocol.Const;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;


public class To0To1ServerApp {
  private static final int RV_PORT = 8040;
  private static final String DB_PORT = "8050";
  private static final String DB_HOST = "localhost";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private static final String WEB_PATH = "/fdo/100/msg";
  private static final String DB_PATH = Path.of(System.getProperty("user.dir"),
      "target", "data", "rvs").toString();
  private static final String SERVER_PATH = Path.of(System.getProperty("user.dir"),
      "target", "tomcat").toString();

  private static String getMessagePath(int msgId) {
    return WEB_PATH + "/" + Integer.toString(msgId);
  }

  /**
   * Runs the Owner Application service.
   */
  public static void main(String[] args) {
    Tomcat tomcat = new Tomcat();

    tomcat.setPort(RV_PORT);

    System.setProperty("catalina.home", SERVER_PATH);

    Context ctx = tomcat.addContext("", null);

    ctx.addParameter("db.url",
        "jdbc:h2:tcp://" + DB_HOST + ":" + DB_PORT + "/" + DB_PATH);
    ctx.addParameter("db.user", DB_USER);
    ctx.addParameter("db.password", DB_PASSWORD);

    try {
      Properties properties = new Properties();
      try (InputStream is = new FileInputStream("application.properties")) {
        properties.load(is);
      }
      ctx.addParameter("epid_test_mode", properties.getProperty("epid_test_mode"));
    } catch (IOException ex) {
      // set default
      ctx.addParameter("epid_test_mode", "false");
    }

    // To enable remote connections to the DB set
    // db.tcpServer=-tcp -tcpAllowOthers -ifNotExists -tcpPort
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("db.tcpServer",
        "-tcp -ifNotExists -tcpPort " + DB_PORT);

    // To enable remote connections to the DB set webAllowOthers=true
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("webAllowOthers", "false");
    ctx.addParameter("trace", "");

    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(To0To1ContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    Wrapper wrapper = tomcat.addServlet(ctx, "rvServlet", new ProtocolServlet());

    wrapper.addMapping(getMessagePath(Const.TO0_HELLO));
    wrapper.addMapping(getMessagePath(Const.TO0_OWNER_SIGN));
    wrapper.addMapping(getMessagePath(Const.TO1_HELLO_RV));
    wrapper.addMapping(getMessagePath(Const.TO1_PROVE_TO_RV));
    wrapper.addMapping(getMessagePath(Const.ERROR));
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "H2Console", new WebServlet());
    wrapper.addMapping("/console/*");
    wrapper.setLoadOnStartup(3);

    tomcat.getConnector();
    try {
      tomcat.start();
    } catch (LifecycleException e) {
      throw new RuntimeException(e);
    }
  }
}
