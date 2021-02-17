// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fido.iot.api.AssignCustomerServlet;
import org.fido.iot.api.DiApiServlet;
import org.fido.iot.protocol.Const;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

/**
 * Runs the Device Initialization Application service.
 */
public class DiApp {

  private static final int DI_PORT = 8039;
  private static final String DB_PORT = "8049";
  private static final String DB_HOST = "localhost";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private static final String WEB_PATH = "/fido/100/msg";
  private static final String DB_PATH = Path.of(System.getProperty("user.dir"),
      "target", "data", "mfg").toString();
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
    System.setProperty("catalina.home", SERVER_PATH);

    Context ctx = tomcat.addContext("", null);

    ctx.addParameter("db.url",
        "jdbc:h2:tcp://" + DB_HOST + ":" + DB_PORT + "/" + DB_PATH);
    ctx.addParameter("db.user", DB_USER);
    ctx.addParameter("db.password", DB_PASSWORD);

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

    // OnDie cert cache is included with the protocol samples.
    Path odcPath = Paths.get(System.getProperty("user.dir"),"../", "onDieCache");

    ctx.addParameter("ods.cacheDir", odcPath.toString());
    ctx.addParameter("ods.autoUpdate", "false");
    ctx.addParameter("ods.zipArtifactUrl", "");
    ctx.addParameter("ods.checkRevocations",
            System.getProperty("ods.checkrevocations", "true"));

    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(DiContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    Wrapper wrapper = tomcat.addServlet(ctx, "diServlet", new ProtocolServlet());

    //wrapper.addInitParameter(Const.);
    wrapper.addMapping(getMessagePath(Const.DI_APP_START));
    wrapper.addMapping(getMessagePath(Const.DI_SET_HMAC));
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "DiApi", new DiApiServlet());
    wrapper.addMapping("/api/v1/vouchers/*");

    wrapper = tomcat.addServlet(ctx, "AssignCustomerApi", new AssignCustomerServlet());
    wrapper.addMapping("/api/v1/assign/*");

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
