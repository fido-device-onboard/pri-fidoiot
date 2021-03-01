// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.fido.iot.protocol.Const;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

public class RvServerApp {

  private static final int RV_HTTP_PORT =
      null != RvConfigLoader.loadConfig(RvAppSettings.TO0_TO1_PORT)
          ? Integer.parseInt(RvConfigLoader.loadConfig(RvAppSettings.TO0_TO1_PORT))
          : 8040;

  private static final int RV_HTTPS_PORT =
      null != RvConfigLoader.loadConfig(RvAppSettings.TO0_TO1_HTTPS_PORT)
          ? Integer.parseInt(RvConfigLoader.loadConfig(RvAppSettings.TO0_TO1_HTTPS_PORT))
          : 8443;

  private static final String RV_SCHEME =
      null != RvConfigLoader.loadConfig(RvAppSettings.TO0_TO1_SCHEME)
          ? RvConfigLoader.loadConfig(RvAppSettings.TO0_TO1_SCHEME) : "http";

  private static String getMessagePath(int msgId) {
    return RvAppSettings.WEB_PATH + "/" + Integer.toString(msgId);
  }

  /** Runs the RV Application service. */
  public static void main(String[] args) {
    Tomcat tomcat = new Tomcat();

    System.setProperty(
        RvAppSettings.SERVER_PATH,
        Path.of(RvConfigLoader.loadConfig(RvAppSettings.SERVER_PATH)).toAbsolutePath().toString());

    Context ctx = tomcat.addContext("", null);

    ctx.addParameter(RvAppSettings.DB_URL, RvConfigLoader.loadConfig(RvAppSettings.DB_URL));
    ctx.addParameter(RvAppSettings.DB_USER, RvConfigLoader.loadConfig(RvAppSettings.DB_USER));
    ctx.addParameter(RvAppSettings.DB_PWD, RvConfigLoader.loadConfig(RvAppSettings.DB_PWD));

    // To enable remote connections to the DB set
    // db.tcpServer=-tcp -tcpAllowOthers -ifNotExists -tcpPort
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter(
        "db.tcpServer",
        "-tcp -ifNotExists -tcpPort " + RvConfigLoader.loadConfig(RvAppSettings.DB_PORT));

    // To enable remote connections to the DB set webAllowOthers=true
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("webAllowOthers", "false");
    ctx.addParameter("trace", "");
    if (null != RvConfigLoader.loadConfig(RvAppSettings.EPID_URL)) {
      ctx.addParameter(RvAppSettings.EPID_URL, RvConfigLoader.loadConfig(RvAppSettings.EPID_URL));
    }
    if (null != RvConfigLoader.loadConfig(RvAppSettings.EPID_TEST_MODE)) {
      ctx.addParameter(RvAppSettings.EPID_TEST_MODE,
              RvConfigLoader.loadConfig(RvAppSettings.EPID_TEST_MODE));
    }

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

    Service service = tomcat.getService();
    Connector httpsConnector = new Connector();

    if (RV_SCHEME.equals("https")) {

      httpsConnector.setPort(RV_HTTPS_PORT);
      httpsConnector.setSecure(true);
      httpsConnector.setScheme("https");

      Path keyStoreFile = Path.of(RvConfigLoader.loadConfig(RvAppSettings.SSL_KEYSTORE_PATH));
      String keystorePass = RvConfigLoader.loadConfig(RvAppSettings.SSL_KEYSTORE_PASSWORD);

      httpsConnector.setProperty("keystorePass", keystorePass);
      httpsConnector.setProperty("keystoreFile", keyStoreFile.toFile().getAbsolutePath());
      httpsConnector.setProperty("clientAuth", "false");
      httpsConnector.setProperty("sslProtocol", "TLS");
      httpsConnector.setProperty("SSLEnabled", "true");
      service.addConnector(httpsConnector);
      tomcat.setConnector(httpsConnector);
    }

    Connector httpConnector = new Connector();
    httpConnector.setPort(RV_HTTP_PORT);
    httpConnector.setScheme("http");
    httpConnector.setRedirectPort(RV_HTTPS_PORT);
    httpConnector.setProperty("protocol","HTTP/1.1");
    httpConnector.setProperty("connectionTimeout","20000");
    service.addConnector(httpConnector);

    if (RV_SCHEME.equals("http")) {
      tomcat.setConnector(httpConnector);
    }

    tomcat.getConnector();
    try {
      tomcat.start();
    } catch (LifecycleException e) {
      throw new RuntimeException(e);
    }
  }
}
