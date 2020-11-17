// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fido.iot.protocol.Const;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

/**
 * Runs the Device Initialization Application service.
 */
public class ManufacturerApp {

  private static final int DI_PORT = null != ManufacturerConfigLoader
      .loadConfig(ManufacturerAppSettings.DI_PORT)
          ? Integer.parseInt(ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DI_PORT))
          : 8039;

  private static String getMessagePath(int msgId) {
    return ManufacturerAppSettings.WEB_PATH + "/" + Integer.toString(msgId);
  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args) {
    Security.addProvider(new BouncyCastleProvider());

    System.out.println(System.getProperty("java.home"));
    try {
      Provider[] providers = Security.getProviders();
      for (int i = 0; i < providers.length; i++) {
        System.out.println(providers[i]);
      }
    } catch (Exception e) {
      System.out.println(e);
    }

    Tomcat tomcat = new Tomcat();
    tomcat.setPort(DI_PORT);

    //set the path of tomcat
    System.setProperty(ManufacturerAppSettings.SERVER_PATH,
        Path.of(ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.SERVER_PATH))
        .toAbsolutePath().toString());

    Context ctx = tomcat.addContext("", null);

    ctx.addParameter(ManufacturerAppSettings.DB_URL,
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DB_URL));
    ctx.addParameter(ManufacturerAppSettings.DB_USER,
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DB_USER));
    ctx.addParameter(ManufacturerAppSettings.DB_PWD,
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DB_PWD));
    ctx.addParameter("db.tcpServer", "-tcp -tcpAllowOthers -ifNotExists -tcpPort "
        + ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DB_PORT));

    ctx.addParameter("webAllowOthers", "true");
    ctx.addParameter("trace", "");

    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(ManufacturerContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    Wrapper wrapper = tomcat.addServlet(ctx, "diServlet", new ProtocolServlet());

    //wrapper.addInitParameter(Const.);
    wrapper.addMapping(getMessagePath(Const.DI_APP_START));
    wrapper.addMapping(getMessagePath(Const.DI_SET_HMAC));
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "DiApi", new ManufacturerApiSevlet());
    wrapper.addMapping("/api/v1/vouchers/*");

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
