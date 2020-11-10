// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.fido.iot.protocol.Const;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

public class RvServerApp {

  private static final int RV_PORT =
      null != RvConfigLoader.loadConfig(RvAppSettings.TO0_TO1_PORT)
          ? Integer.parseInt(RvConfigLoader.loadConfig(RvAppSettings.TO0_TO1_PORT))
          : 8040;

  private static String getMessagePath(int msgId) {
    return RvAppSettings.WEB_PATH + "/" + Integer.toString(msgId);
  }

  /** Runs the RV Application service. */
  public static void main(String[] args) {
    Tomcat tomcat = new Tomcat();

    tomcat.setPort(RV_PORT);

    System.setProperty(
        RvAppSettings.SERVER_PATH,
        Path.of(RvConfigLoader.loadConfig(RvAppSettings.SERVER_PATH)).toAbsolutePath().toString());

    Context ctx = tomcat.addContext("", null);

    ctx.addParameter(RvAppSettings.DB_URL, RvConfigLoader.loadConfig(RvAppSettings.DB_URL));
    ctx.addParameter(RvAppSettings.DB_USER, RvConfigLoader.loadConfig(RvAppSettings.DB_USER));
    ctx.addParameter(RvAppSettings.DB_PWD, RvConfigLoader.loadConfig(RvAppSettings.DB_PWD));
    ctx.addParameter(
        "db.tcpServer",
        "-tcp -tcpAllowOthers -ifNotExists -tcpPort "
            + RvConfigLoader.loadConfig(RvAppSettings.DB_PORT));

    ctx.addParameter("webAllowOthers", "true");
    ctx.addParameter("trace", "");

    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(RvContextListener.class.getName());
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
