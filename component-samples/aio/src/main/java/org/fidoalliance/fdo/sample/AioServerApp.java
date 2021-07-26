// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.nio.file.Path;
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
import org.fidoalliance.fdo.api.OwnerCustomerServlet;
import org.fidoalliance.fdo.api.OwnerReplacementVoucherServlet;
import org.fidoalliance.fdo.api.OwnerSetupInfoServlet;
import org.fidoalliance.fdo.api.OwnerSviSettingsServlet;
import org.fidoalliance.fdo.api.OwnerSystemResourceServlet;
import org.fidoalliance.fdo.api.OwnerVoucherServlet;
import org.fidoalliance.fdo.api.RvInfoServlet;
import org.fidoalliance.fdo.protocol.Const;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

public class AioServerApp {

  private static final int AIO_PORT = null != AioConfigLoader
      .loadConfig(AioAppSettings.AIO_PORT)
      ? Integer.parseInt(AioConfigLoader.loadConfig(AioAppSettings.AIO_PORT))
      : 8080;

  private static final int AIO_HTTPS_PORT =
      null != AioConfigLoader.loadConfig(AioAppSettings.AIO_HTTPS_PORT)
          ? Integer.parseInt(AioConfigLoader.loadConfig(
          AioAppSettings.AIO_HTTPS_PORT)) : 443;

  private static final String AIO_SCHEME =
      null != AioConfigLoader.loadConfig(AioAppSettings.AIO_SCHEME)
          ? AioConfigLoader.loadConfig(AioAppSettings.AIO_SCHEME) : "http";

  private static String getMessagePath(int msgId) {
    return AioAppSettings.WEB_PATH + "/" + Integer.toString(msgId);
  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args) {
    Security.addProvider(new BouncyCastleProvider());
    Tomcat tomcat = new Tomcat();

    // set the path of tomcat
    System.setProperty(AioAppSettings.SERVER_PATH,
        Path.of(AioConfigLoader.loadConfig(AioAppSettings.SERVER_PATH)).toAbsolutePath()
            .toString());

    tomcat.setAddDefaultWebXmlToWebapp(false);
    Context ctx = tomcat.addWebapp("", System.getProperty(AioAppSettings.SERVER_PATH));

    ctx.addParameter(AioAppSettings.DB_URL,
        AioConfigLoader.loadConfig(AioAppSettings.DB_URL));
    ctx.addParameter(AioAppSettings.DB_USER,
        AioConfigLoader.loadConfig(AioAppSettings.DB_USER));
    ctx.addParameter(AioAppSettings.DB_PWD,
        AioConfigLoader.loadConfig(AioAppSettings.DB_PWD));
    ctx.addParameter(AioAppSettings.DB_DRIVER,
        AioConfigLoader.loadConfig(AioAppSettings.DB_DRIVER));
    ctx.addParameter(AioAppSettings.DB_INIT_SQL,
        AioConfigLoader.loadConfig(AioAppSettings.DB_INIT_SQL));
    ctx.addParameter(AioAppSettings.DB_NEW_DEVICE_SQL,
        AioConfigLoader.loadConfig(AioAppSettings.DB_NEW_DEVICE_SQL));
    ctx.addParameter(AioAppSettings.DB_SESSION_CHECK_INTERVAL,
        AioConfigLoader.loadConfig(AioAppSettings.DB_SESSION_CHECK_INTERVAL));

    // hard-coded H2 config
    // To enable remote connections to the DB set
    // db.tcpServer=-tcp -tcpAllowOthers -ifNotExists -tcpPort
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("db.tcpServer", "-tcp -ifNotExists -tcpPort "
        + AioConfigLoader.loadConfig(AioAppSettings.DB_PORT));

    try {
      ctx.addParameter(AioAppSettings.ONDIE_CACHEDIR,
          AioConfigLoader.loadConfig(AioAppSettings.ONDIE_CACHEDIR));
      ctx.addParameter(AioAppSettings.ONDIE_AUTOUPDATE,
          AioConfigLoader.loadConfig(AioAppSettings.ONDIE_AUTOUPDATE));
      ctx.addParameter(AioAppSettings.ONDIE_ZIP_ARTIFACT,
          AioConfigLoader.loadConfig(AioAppSettings.ONDIE_ZIP_ARTIFACT));
      ctx.addParameter(AioAppSettings.ONDIE_CHECK_REVOCATIONS,
          AioConfigLoader.loadConfig(AioAppSettings.ONDIE_CHECK_REVOCATIONS));
    } catch (Exception ex) {
      // ondie is optional so if config cannot be loaded just default to no config
    }

    if (null != AioConfigLoader.loadConfig(AioAppSettings.EPID_URL)) {
      ctx.addParameter(AioAppSettings.EPID_URL,
          AioConfigLoader.loadConfig(AioAppSettings.EPID_URL));
    }
    if (null != AioConfigLoader.loadConfig(AioAppSettings.EPID_TEST_MODE)) {
      ctx.addParameter(AioAppSettings.EPID_TEST_MODE,
          AioConfigLoader.loadConfig(AioAppSettings.EPID_TEST_MODE));
    }
    ctx.addParameter(AioAppSettings.OWNER_KEYSTORE,
        AioConfigLoader.loadConfig(AioAppSettings.OWNER_KEYSTORE));
    ctx.addParameter(AioAppSettings.OWNER_KEYSTORE_PWD,
        AioConfigLoader.loadConfig(AioAppSettings.OWNER_KEYSTORE_PWD));
    ctx.addParameter(AioAppSettings.OWNER_KEYSTORE_TYPE,
        AioConfigLoader.loadConfig(AioAppSettings.OWNER_KEYSTORE_TYPE));

    ctx.addParameter(AioAppSettings.OWNER_REPLACEMENT_KEYS,
        AioConfigLoader.loadConfig(AioAppSettings.OWNER_REPLACEMENT_KEYS));
    ctx.addParameter(AioAppSettings.OWNER_TRANSFER_KEYS,
        AioConfigLoader.loadConfig(AioAppSettings.OWNER_TRANSFER_KEYS));

    ctx.addParameter(AioAppSettings.TO0_RV_BLOB,
        AioConfigLoader.loadConfig(AioAppSettings.TO0_RV_BLOB));

    ctx.addParameter(AioAppSettings.MANUFACTURER_KEYSTORE,
        AioConfigLoader.loadConfig(AioAppSettings.MANUFACTURER_KEYSTORE));
    ctx.addParameter(AioAppSettings.MANUFACTURER_KEYSTORE_PWD,
        AioConfigLoader.loadConfig(AioAppSettings.MANUFACTURER_KEYSTORE_PWD));
    ctx.addParameter(AioAppSettings.MANUFACTURER_KEYSTORE_TYPE,
        AioConfigLoader.loadConfig(AioAppSettings.MANUFACTURER_KEYSTORE_TYPE));

    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(AioContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    //load owner protocol servlet and api
    Wrapper wrapper = tomcat.addServlet(ctx, "fdoServlet", new ProtocolServlet());

    wrapper.addMapping(getMessagePath(Const.TO2_HELLO_DEVICE));
    wrapper.addMapping(getMessagePath(Const.TO2_GET_OVNEXT_ENTRY));
    wrapper.addMapping(getMessagePath(Const.TO2_PROVE_DEVICE));
    wrapper.addMapping(getMessagePath(Const.TO2_DEVICE_SERVICE_INFO_READY));
    wrapper.addMapping(getMessagePath(Const.TO2_DEVICE_SERVICE_INFO));
    wrapper.addMapping(getMessagePath(Const.TO2_DONE));

    wrapper.addMapping(getMessagePath(Const.TO0_HELLO));
    wrapper.addMapping(getMessagePath(Const.TO0_OWNER_SIGN));
    wrapper.addMapping(getMessagePath(Const.TO1_HELLO_RV));
    wrapper.addMapping(getMessagePath(Const.TO1_PROVE_TO_RV));

    wrapper.addMapping(getMessagePath(Const.DI_APP_START));
    wrapper.addMapping(getMessagePath(Const.DI_SET_HMAC));
    wrapper.addMapping(getMessagePath(Const.ERROR));
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "voucherServlet",
        new OwnerVoucherServlet());
    wrapper.setAsyncSupported(true);
    wrapper.addMapping("/api/v1/owner/vouchers/*");
    wrapper = tomcat.addServlet(ctx, "replacementVoucherServlet",
        new OwnerReplacementVoucherServlet());
    wrapper.addMapping("/api/v1/owner/newvoucher/*");
    wrapper = tomcat.addServlet(ctx, "setupinfoServlet",
        new OwnerSetupInfoServlet());
    wrapper.addMapping("/api/v1/owner/setupinfo/*");
    wrapper.setAsyncSupported(true);
    wrapper = tomcat.addServlet(ctx, "ownerCustomerServlet",
        new OwnerCustomerServlet());
    wrapper.addMapping("/api/v1/owner/customer/*");

    wrapper = tomcat.addServlet(ctx, "SystemResource", new OwnerSystemResourceServlet());
    wrapper.setAsyncSupported(true);
    wrapper.addMapping("/api/v1/device/svi");

    wrapper = tomcat.addServlet(ctx, "sviSettingsServlet",
        new OwnerSviSettingsServlet());
    wrapper.addMapping("/api/v1/owner/svi/settings/*");

    wrapper = tomcat.addServlet(ctx, "DiApi", new DiApiServlet());
    wrapper.addMapping("/api/v1/vouchers/*");
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "AioInfoServlet", new AioInfoServlet());
    wrapper.addMapping("/api/v1/deviceinfo/*");
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "AssignCustomerApi", new AssignCustomerServlet());
    wrapper.addMapping("/api/v1/assign/*");
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "UpdateRvInfoApi", new RvInfoServlet());
    wrapper.addMapping("/api/v1/rvinfo/*");
    wrapper.setAsyncSupported(true);
    //wrapper.setLoadOnStartup(4);

    wrapper = tomcat.addServlet(ctx, "downloadServlet", new AioFileDownloadServlet());
    wrapper.addMapping("/downloads/*");
    wrapper.addInitParameter(AioAppSettings.DOWNLOADS_PATH,
        AioConfigLoader.loadConfig(AioAppSettings.DOWNLOADS_PATH));
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "uploadServlet", new AioFileUploadServlet());
    wrapper.addMapping("/api/v1/uploads/*");
    wrapper.addInitParameter(AioAppSettings.DOWNLOADS_PATH,
        AioConfigLoader.loadConfig(AioAppSettings.DOWNLOADS_PATH));
    wrapper.setAsyncSupported(true);


    wrapper = tomcat.addServlet(ctx, "H2Console", new WebServlet());
    wrapper.addMapping("/console/*");
    // To enable remote connections to the DB set webAllowOthers=true
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    wrapper.addInitParameter("webAllowOthers",
        AioConfigLoader.loadConfig(AioAppSettings.DB_ALLOW_OTHERS));
    //wrapper.addInitParameter("trace", "");
    wrapper.setLoadOnStartup(3);

    //setup digest auth
    LoginConfig config = new LoginConfig();
    config.setAuthMethod(AioAppSettings.AUTH_METHOD);
    ctx.setLoginConfig(config);
    ctx.addSecurityRole(AioAppSettings.AUTH_ROLE);
    SecurityConstraint constraint = new SecurityConstraint();
    constraint.addAuthRole(AioAppSettings.AUTH_ROLE);
    SecurityCollection collection = new SecurityCollection();
    collection.addPattern("/api/v1/*");
    constraint.addCollection(collection);
    ctx.addConstraint(constraint);
    tomcat.addRole(AioConfigLoader.loadConfig(AioAppSettings.API_USER),
        AioAppSettings.AUTH_ROLE);
    tomcat.addUser(AioConfigLoader.loadConfig(AioAppSettings.API_USER),
        AioConfigLoader.loadConfig(AioAppSettings.API_PWD));

    Service service = tomcat.getService();
    Connector httpsConnector = new Connector();

    if (AIO_SCHEME.toLowerCase().equals("https")) {

      httpsConnector.setPort(AIO_HTTPS_PORT);
      httpsConnector.setSecure(true);
      httpsConnector.setScheme(AIO_SCHEME);

      Path keyStoreFile =
          Path.of(AioConfigLoader.loadConfig(AioAppSettings.SSL_KEYSTORE_PATH));
      String keystorePass =
          AioConfigLoader.loadConfig(AioAppSettings.SSL_KEYSTORE_PASSWORD);

      httpsConnector.setProperty("keystorePass", keystorePass);
      httpsConnector.setProperty("keystoreFile", keyStoreFile.toFile().getAbsolutePath());
      httpsConnector.setProperty("clientAuth", "false");
      httpsConnector.setProperty("sslProtocol", "TLS");
      httpsConnector.setProperty("SSLEnabled", "true");
      service.addConnector(httpsConnector);

    }

    Connector httpConnector = new Connector();
    httpConnector.setPort(AIO_PORT);
    httpConnector.setScheme("http");
    httpConnector.setRedirectPort(AIO_HTTPS_PORT);
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
