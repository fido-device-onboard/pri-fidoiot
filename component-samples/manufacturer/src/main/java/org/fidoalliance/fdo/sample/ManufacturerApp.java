// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fidoalliance.fdo.api.AssignCustomerServlet;
import org.fidoalliance.fdo.api.DiApiServlet;
import org.fidoalliance.fdo.api.RvInfoServlet;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;
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

  private static final int DI_HTTPS_PORT =
      null != ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DI_HTTPS_PORT)
          ? Integer.parseInt(ManufacturerConfigLoader.loadConfig(
          ManufacturerAppSettings.DI_HTTPS_PORT)) : 443;

  private static final String DI_SCHEME =
      null != ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DI_SCHEME)
          ? ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DI_SCHEME) : "http";

  private static String getMessagePath(int msgId) {
    return ManufacturerAppSettings.WEB_PATH + "/" + Integer.toString(msgId);
  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args) {

    LoggerService logger = new LoggerService(ManufacturerApp.class);
    Security.addProvider(new BouncyCastleProvider());

    logger.info(System.getProperty("java.home"));
    try {
      Provider[] providers = Security.getProviders();
      for (int i = 0; i < providers.length; i++) {
        logger.info((providers[i]));
      }
    } catch (Exception e) {
      logger.error(e.toString());
    }

    Tomcat tomcat = new Tomcat();

    //set the path of tomcat
    System.setProperty(ManufacturerAppSettings.SERVER_PATH,
        Path.of(ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.SERVER_PATH))
        .toAbsolutePath().toString());

    tomcat.setAddDefaultWebXmlToWebapp(false);
    Context ctx = tomcat.addWebapp("", System.getProperty(ManufacturerAppSettings.SERVER_PATH));

    ctx.addParameter(ManufacturerAppSettings.DB_URL,
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DB_URL));
    ctx.addParameter(ManufacturerAppSettings.DB_USER,
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DB_USER));
    ctx.addParameter(ManufacturerAppSettings.DB_PWD,
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DB_PWD));

    // To enable remote connections to the DB set
    // db.tcpServer=-tcp -tcpAllowOthers -ifNotExists -tcpPort
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("db.tcpServer", "-tcp -ifNotExists -tcpPort "
        + ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.DB_PORT));

    // To enable remote connections to the DB set webAllowOthers=true
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("webAllowOthers", "false");
    ctx.addParameter("trace", "");

    try {
      ctx.addParameter(ManufacturerAppSettings.OWNER_PUB_KEY_PATH,
              ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.OWNER_PUB_KEY_PATH));
    } catch (Exception ex) {
      // Default Owner public keys are optional. If config can't be loaded,default to no config.
    }

    try {
      ctx.addParameter(ManufacturerAppSettings.RESELLER_PUB_KEY_PATH,
              ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.RESELLER_PUB_KEY_PATH));
    } catch (Exception ex) {
      // Default Reseller public keys are optional. If config can't be loaded,default to no config.
    }

    try {
      ctx.addParameter(ManufacturerAppSettings.ONDIE_CACHEDIR,
              ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.ONDIE_CACHEDIR));
      ctx.addParameter(ManufacturerAppSettings.ONDIE_AUTOUPDATE,
              ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.ONDIE_AUTOUPDATE));
      ctx.addParameter(ManufacturerAppSettings.ONDIE_SOURCE_URLS,
              ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.ONDIE_SOURCE_URLS));
      ctx.addParameter(ManufacturerAppSettings.ONDIE_CHECK_REVOCATIONS,
              ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.ONDIE_CHECK_REVOCATIONS));
    } catch (Exception ex) {
      // ondie is optional so if config cannot be loaded just default to no config
    }

    ctx.addParameter(ManufacturerAppSettings.MFG_KEYSTORE,
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.MFG_KEYSTORE));
    ctx.addParameter(ManufacturerAppSettings.MFG_KEYSTORE_PWD,
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.MFG_KEYSTORE_PWD));
    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(ManufacturerContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    //setup digest auth
    LoginConfig config = new LoginConfig();
    config.setAuthMethod(ManufacturerAppSettings.AUTH_METHOD);
    ctx.setLoginConfig(config);
    ctx.addSecurityRole(ManufacturerAppSettings.AUTH_ROLE);
    SecurityConstraint constraint = new SecurityConstraint();
    constraint.addAuthRole(ManufacturerAppSettings.AUTH_ROLE);
    SecurityCollection collection = new SecurityCollection();
    collection.addPattern("/api/v1/*");
    constraint.addCollection(collection);
    ctx.addConstraint(constraint);
    tomcat.addRole(ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.API_USER),
        ManufacturerAppSettings.AUTH_ROLE);
    tomcat.addUser(ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.API_USER),
        ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.API_PWD));

    Wrapper wrapper = tomcat.addServlet(ctx, "diServlet", new ProtocolServlet());

    wrapper.addMapping(getMessagePath(Const.DI_APP_START));
    wrapper.addMapping(getMessagePath(Const.DI_SET_HMAC));
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "DiApi", new DiApiServlet());
    wrapper.addMapping("/api/v1/vouchers/*");

    wrapper = tomcat.addServlet(ctx, "mfgCustomer",
        new ManufacturerCustomerServlet());
    wrapper.addMapping("/api/v1/customers/*");
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "AssignCustomerApi", new AssignCustomerServlet());
    wrapper.addMapping("/api/v1/assign/*");

    wrapper = tomcat.addServlet(ctx, "UpdateRvInfoApi", new RvInfoServlet());
    wrapper.addMapping("/api/v1/rvinfo/*");

    wrapper = tomcat.addServlet(ctx, "H2Console", new WebServlet());
    wrapper.addMapping("/console/*");
    wrapper.setLoadOnStartup(3);

    Service service = tomcat.getService();
    Connector httpsConnector = new Connector();

    if (DI_SCHEME.toLowerCase().equals("https")) {

      httpsConnector.setPort(DI_HTTPS_PORT);
      httpsConnector.setSecure(true);
      httpsConnector.setScheme(DI_SCHEME);

      Path keyStoreFile =
          Path.of(ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.SSL_KEYSTORE_PATH));
      String keystorePass =
          ManufacturerConfigLoader.loadConfig(ManufacturerAppSettings.SSL_KEYSTORE_PASSWORD);

      httpsConnector.setProperty("keystorePass", keystorePass);
      httpsConnector.setProperty("keystoreFile", keyStoreFile.toFile().getAbsolutePath());
      httpsConnector.setProperty("clientAuth", "false");
      httpsConnector.setProperty("sslProtocol", "TLS");
      httpsConnector.setProperty("SSLEnabled", "true");
      service.addConnector(httpsConnector);

    }

    Connector httpConnector = new Connector();
    httpConnector.setPort(DI_PORT);
    httpConnector.setScheme("http");
    httpConnector.setRedirectPort(DI_HTTPS_PORT);
    httpConnector.setProperty("protocol", "HTTP/1.1");
    httpConnector.setProperty("connectionTimeout", "20000");
    service.addConnector(httpConnector);
    tomcat.setConnector(httpConnector);

    tomcat.getConnector();
    try {

      tomcat.start();
    } catch (LifecycleException e) {
      throw new RuntimeException(e);
    }
  }

}
