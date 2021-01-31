// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.fido.iot.api.OwnerReplacementVoucherServlet;
import org.fido.iot.api.OwnerServiceInfoValuesServlet;
import org.fido.iot.api.OwnerSetupInfoServlet;
import org.fido.iot.api.OwnerSviServlet;
import org.fido.iot.api.OwnerVoucherServlet;
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

    tomcat.setAddDefaultWebXmlToWebapp(false);
    Context ctx = tomcat.addWebapp("", System.getProperty(OwnerAppSettings.SERVER_PATH));

    ctx.addParameter(OwnerAppSettings.DB_URL,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_URL));
    ctx.addParameter(OwnerAppSettings.DB_USER,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_USER));
    ctx.addParameter(OwnerAppSettings.DB_PWD,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_PWD));

    // hard-coded H2 config
    // To enable remote connections to the DB set
    // db.tcpServer=-tcp -tcpAllowOthers -ifNotExists -tcpPort
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("db.tcpServer", "-tcp -ifNotExists -tcpPort "
        + OwnerConfigLoader.loadConfig(OwnerAppSettings.DB_PORT));

    // To enable remote connections to the DB set webAllowOthers=true
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("webAllowOthers", "false");
    ctx.addParameter("trace", "");

    if (null != OwnerConfigLoader.loadConfig(OwnerAppSettings.EPID_URL)) {
      ctx.addParameter(OwnerAppSettings.EPID_URL,
          OwnerConfigLoader.loadConfig(OwnerAppSettings.EPID_URL));
    }
    if (null != OwnerConfigLoader.loadConfig(OwnerAppSettings.EPID_TEST_MODE)) {
      ctx.addParameter(OwnerAppSettings.EPID_TEST_MODE,
          OwnerConfigLoader.loadConfig(OwnerAppSettings.EPID_TEST_MODE));
    }
    ctx.addParameter(OwnerAppSettings.OWNER_KEYSTORE,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.OWNER_KEYSTORE));
    ctx.addParameter(OwnerAppSettings.OWNER_KEYSTORE_PWD,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.OWNER_KEYSTORE_PWD));
    ctx.addParameter(OwnerAppSettings.TO0_SCHEDULING_ENABLED,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.TO0_SCHEDULING_ENABLED));
    ctx.addParameter(OwnerAppSettings.TO0_SCHEDULING_INTREVAL,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.TO0_SCHEDULING_INTREVAL));
    ctx.addParameter(OwnerAppSettings.TO0_RV_BLOB,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.TO0_RV_BLOB));
    ctx.addParameter(OwnerAppSettings.SAMPLE_SVI_PATH,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.SAMPLE_SVI_PATH));
    ctx.addParameter(OwnerAppSettings.SAMPLE_VALUES_PATH,
        OwnerConfigLoader.loadConfig(OwnerAppSettings.SAMPLE_VALUES_PATH));
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

    wrapper = tomcat.addServlet(ctx, "voucherServlet",
        new OwnerVoucherServlet());
    wrapper.addMapping("/api/v1/owner/vouchers/*");
    wrapper = tomcat.addServlet(ctx, "replacementVoucherServlet",
        new OwnerReplacementVoucherServlet());
    wrapper.addMapping("/api/v1/owner/newvoucher/*");
    wrapper = tomcat.addServlet(ctx, "serviceinfoServlet",
        new OwnerServiceInfoValuesServlet());
    wrapper.addMapping("/api/v1/owner/svivalues/*");
    wrapper = tomcat.addServlet(ctx, "sviServlet",
        new OwnerSviServlet());
    wrapper.addMapping("/api/v1/owner/svi/*");
    wrapper = tomcat.addServlet(ctx, "setupinfoServlet",
        new OwnerSetupInfoServlet());
    wrapper.addMapping("/api/v1/owner/setupinfo/*");
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "H2Console", new WebServlet());
    wrapper.addMapping("/console/*");
    wrapper.setLoadOnStartup(3);

    //setup digest auth
    LoginConfig config = new LoginConfig();
    config.setAuthMethod(OwnerAppSettings.AUTH_METHOD);
    ctx.setLoginConfig(config);
    ctx.addSecurityRole(OwnerAppSettings.AUTH_ROLE);
    SecurityConstraint constraint = new SecurityConstraint();
    constraint.addAuthRole(OwnerAppSettings.AUTH_ROLE);
    SecurityCollection collection = new SecurityCollection();
    collection.addPattern("/api/v1/owner/*");
    constraint.addCollection(collection);
    ctx.addConstraint(constraint);
    tomcat.addRole(OwnerConfigLoader.loadConfig(OwnerAppSettings.API_USER),
        OwnerAppSettings.AUTH_ROLE);
    tomcat.addUser(OwnerConfigLoader.loadConfig(OwnerAppSettings.API_USER),
        OwnerConfigLoader.loadConfig(OwnerAppSettings.API_PWD));

    tomcat.getConnector();
    try {
      tomcat.start();
    } catch (LifecycleException e) {
      throw new RuntimeException(e);
    }
  }
}
