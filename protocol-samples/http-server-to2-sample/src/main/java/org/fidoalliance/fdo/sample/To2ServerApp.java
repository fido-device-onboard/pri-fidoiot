// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.fidoalliance.fdo.api.OwnerSviSettingsServlet;
import org.fidoalliance.fdo.api.OwnerSystemResourceServlet;
import org.fidoalliance.fdo.api.OwnerVoucherServlet;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

/**
 * Runs the Owner Application service.
 */
public class To2ServerApp {

  private static final int TO2_PORT = 8042;
  private static final String DB_PORT = "8051";
  private static final String DB_HOST = "localhost";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private static final String WEB_PATH = "/fdo/100/msg";
  private static final String DB_PATH = Path.of(System.getProperty("user.dir"),
      "target", "data", "ops").toString();
  private static final String SERVER_PATH = Path.of(System.getProperty("user.dir"),
      "target", "tomcat").toString();
  private static final LoggerService logger = new LoggerService(To2ServerApp.class);

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

    // OnDie cert cache is included with the protocol samples.
    Path odcPath = Paths.get(System.getProperty("user.dir"), "../", "onDieCache");

    logger.info("Working Directory = " + odcPath.toString());
    ctx.addParameter("ods.cacheDir", odcPath.toUri().toString());
    ctx.addParameter("ods.autoUpdate", "false");
    ctx.addParameter("ods.zipArtifactUrl", "");
    ctx.addParameter("ods.checkRevocations", "true");

    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(To2ContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    Wrapper wrapper = tomcat.addServlet(ctx, "opsServlet", new ProtocolServlet());

    wrapper.addMapping(getMessagePath(Const.TO2_HELLO_DEVICE));
    wrapper.addMapping(getMessagePath(Const.TO2_GET_OVNEXT_ENTRY));
    wrapper.addMapping(getMessagePath(Const.TO2_PROVE_DEVICE));
    wrapper.addMapping(getMessagePath(Const.TO2_DEVICE_SERVICE_INFO_READY));
    wrapper.addMapping(getMessagePath(Const.TO2_DEVICE_SERVICE_INFO));
    wrapper.addMapping(getMessagePath(Const.TO2_DONE));
    wrapper.addMapping(getMessagePath(Const.ERROR));

    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "OwnerVoucher", new OwnerVoucherServlet());
    wrapper.addMapping("/api/v1/device/vouchers");

    wrapper = tomcat.addServlet(ctx, "SystemResource", new OwnerSystemResourceServlet());
    wrapper.addMapping("/api/v1/device/svi");

    wrapper = tomcat.addServlet(ctx, "sviSettingsServlet",
        new OwnerSviSettingsServlet());
    wrapper.addMapping("/api/v1/owner/svi/settings/*");

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
