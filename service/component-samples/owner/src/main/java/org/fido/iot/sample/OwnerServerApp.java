// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.fido.iot.protocol.Const;
import org.fido.iot.sample.ProtocolServlet;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

/**
 * Runs the Owner Application service.
 */
public class OwnerServerApp {

  private static final int TO2_PORT = null != OwnerConfigLoader
      .loadConfig(OwnerAppSettings.TO2_PORT)
          ? Integer.parseInt(OwnerConfigLoader.loadConfig(OwnerAppSettings.TO2_PORT))
          : 8042;

  private static String getMessagePath(int msgId) {
    return OwnerAppSettings.WEB_PATH + "/" + Integer.toString(msgId);
  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args) {
    Tomcat tomcat = new Tomcat();
    tomcat.setPort(TO2_PORT);
    // set the path of tomcat
    System.setProperty(OwnerAppSettings.SERVER_PATH,
        Path.of(OwnerConfigLoader.loadConfig(OwnerAppSettings.SERVER_PATH)).toAbsolutePath()
            .toString());

    Context ctx = tomcat.addContext("", null);

    ctx.addParameter(OwnerAppSettings.DB_URL,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_URL));
    ctx.addParameter(OwnerAppSettings.DB_USER,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_USER));
    ctx.addParameter(OwnerAppSettings.DB_PWD,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_PWD));

    ctx.addParameter("db.url",
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_URL));
    ctx.addParameter("db.user",
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_USER));
    ctx.addParameter("db.password",
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_PWD));

    // hard-coded H2 config
    ctx.addParameter("db.tcpServer", "-tcp -tcpAllowOthers -ifNotExists -tcpPort "
        + OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_PORT));
    ctx.addParameter("webAllowOthers", "true");
    ctx.addParameter("trace", "");

    if (null != OwnerConfigLoader.loadConfig(OwnerAppSettings.EPID_URL)) {
      ctx.addParameter(OwnerAppSettings.EPID_URL,
          OwnerConfigLoader.loadConfig(OwnerAppSettings.EPID_URL));
    }
    ctx.addParameter(OwnerAppSettings.OWNER_KEYSTORE_PWD,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.OWNER_KEYSTORE_PWD));
    ctx.addParameter(OwnerAppSettings.TO0_SCHEDULING_ENABLED,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.TO0_SCHEDULING_ENABLED));
    ctx.addParameter(OwnerAppSettings.TO0_SCHEDULING_INTREVAL,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.TO0_SCHEDULING_INTREVAL));
    ctx.addParameter(OwnerAppSettings.TO0_RV_BLOB,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.TO0_RV_BLOB));
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
