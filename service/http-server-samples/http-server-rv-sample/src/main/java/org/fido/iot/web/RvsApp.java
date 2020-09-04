// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.web;

import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.fido.iot.protocol.Const;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

public class RvsApp {
  private static final int RV_PORT = 8040;
  private static final String DB_PORT = "8050";
  private static final String DB_HOST = "localhost";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private static final String WEB_PATH = "/fido/100/msg";
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
    ctx.addParameter("db.tcpServer",
        "-tcp -tcpAllowOthers -ifNotExists -tcpPort " + DB_PORT);

    ctx.addParameter("webAllowOthers", "true");
    ctx.addParameter("trace", "");

    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(RvsContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    Wrapper wrapper = tomcat.addServlet(ctx, "rvServlet", new ProtocolServlet());

    wrapper.addMapping(getMessagePath(Const.TO0_HELLO));
    wrapper.addMapping(getMessagePath(Const.TO0_OWNER_SIGN));
    wrapper.addMapping(getMessagePath(Const.TO1_HELLO_RV));
    wrapper.addMapping(getMessagePath(Const.TO1_PROVE_TO_RV));
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
