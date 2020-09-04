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

/**
 * Runs the Owner Application service.
 */
public class OwnerApp {

  private static final int TO2_PORT = 8042;
  private static final String DB_PORT = "8051";
  private static final String DB_HOST = "localhost";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private static final String WEB_PATH = "/fido/100/msg";
  private static final String DB_PATH = Path.of(System.getProperty("user.dir"),
      "target", "data", "ops").toString();
  private static final String SERVER_PATH = Path.of(System.getProperty("user.dir"),
      "target", "tomcat").toString();

  private static String getMessagePath(int msgId) {
    return WEB_PATH + "/" + Integer.toString(msgId);
  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args) {
    Tomcat tomcat = new Tomcat();

    tomcat.setPort(TO2_PORT);

    //set the path of tomcat

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
    ctx.addApplicationListener(OwnerContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    Wrapper wrapper = tomcat.addServlet(ctx, "opsServlet", new ProtocolServlet());

    wrapper.addMapping(getMessagePath(Const.TO2_HELLO_DEVICE));
    wrapper.addMapping(getMessagePath(Const.TO2_GET_OVNEXT_ENTRY));
    wrapper.addMapping(getMessagePath(Const.TO2_PROVE_DEVICE));
    wrapper.addMapping(getMessagePath(Const.TO2_AUTH_DONE));
    wrapper.addMapping(getMessagePath(Const.TO2_DEVICE_SERVICE_INFO));
    wrapper.addMapping(getMessagePath(Const.TO2_DONE));

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
