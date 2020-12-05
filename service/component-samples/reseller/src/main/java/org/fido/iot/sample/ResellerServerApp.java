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
import org.fido.iot.api.KeyStoreServlet;
import org.h2.server.web.DbStarter;
import org.h2.server.web.WebServlet;

public class ResellerServerApp {
  private static final String CATALINA_HOME = "catalina.home";
  private static final String AUTH_ROLE = "api";

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args) {

    Tomcat tomcat = new Tomcat();

    tomcat.setPort(
        Integer.parseInt(ResellerConfigLoader.loadConfig(ResellerAppConstants.API_PORT)));

    System.out.println(System.getProperty("user.dir"));
    // set the path of tomcat
    System.setProperty(CATALINA_HOME,
        Path.of(ResellerConfigLoader.loadConfig(ResellerAppConstants.SERVER_PATH)).toAbsolutePath()
            .toString());

    tomcat.setAddDefaultWebXmlToWebapp(false);
    Context ctx = tomcat.addWebapp("",
        System.getProperty(CATALINA_HOME));

    ctx.addParameter(ResellerAppConstants.DB_URL,
        ResellerConfigLoader.loadConfig(ResellerAppConstants.DB_URL));
    ctx.addParameter(ResellerAppConstants.DB_USER,
        ResellerConfigLoader.loadConfig(ResellerAppConstants.DB_USER));
    ctx.addParameter(ResellerAppConstants.DB_PWD,
        ResellerConfigLoader.loadConfig(ResellerAppConstants.DB_PWD));
    ctx.addParameter(ResellerAppConstants.DB_DRIVER,
        ResellerConfigLoader.loadConfig(ResellerAppConstants.DB_DRIVER));

    // hard-coded H2 config
    // To enable remote connections to the DB set
    // db.tcpServer=-tcp -tcpAllowOthers -ifNotExists -tcpPort
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("db.tcpServer", "-tcp -ifNotExists -tcpPort "
        + ResellerConfigLoader.loadConfig(ResellerAppConstants.DB_PORT));

    // To enable remote connections to the DB set webAllowOthers=true
    // This creates a security hole in the system.
    // Not recommended to use especially on production system
    ctx.addParameter("webAllowOthers", "false");
    ctx.addParameter("trace", "");

    ctx.addParameter(ResellerAppConstants.KEYSTORE_PWD,
        ResellerConfigLoader.loadConfig(ResellerAppConstants.KEYSTORE_PWD));

    ctx.addApplicationListener(DbStarter.class.getName());
    ctx.addApplicationListener(ResellerContextListener.class.getName());
    ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    Wrapper wrapper = tomcat.addServlet(ctx, "resellServlet",
        new ResellerVoucherServlet());

    //add api paths
    wrapper.addMapping("/api/v1/resell/vouchers/*");
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "resellCustomer",
        new ResellerCustomerServlet());

    wrapper.addMapping("/api/v1/resell/customers/*");
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "resellStore",
        new KeyStoreServlet());

    wrapper.addMapping("/api/v1/resell/keys/*");
    wrapper.setAsyncSupported(true);

    wrapper = tomcat.addServlet(ctx, "H2Console", new WebServlet());
    wrapper.addMapping("/console/*");
    wrapper.setLoadOnStartup(3);

    //setup digest auth
    LoginConfig config = new LoginConfig();
    config.setAuthMethod("DIGEST");
    ctx.setLoginConfig(config);
    ctx.addSecurityRole(AUTH_ROLE);
    SecurityConstraint constraint = new SecurityConstraint();
    constraint.addAuthRole(AUTH_ROLE);
    SecurityCollection collection = new SecurityCollection();
    collection.addPattern("/api/*");
    constraint.addCollection(collection);
    ctx.addConstraint(constraint);

    tomcat.addRole("admin", AUTH_ROLE);
    tomcat.addUser(ResellerConfigLoader.loadConfig(ResellerAppConstants.API_USER),
        ResellerConfigLoader.loadConfig(ResellerAppConstants.API_PWD));

    tomcat.getConnector();
    try {
      tomcat.start();
    } catch (LifecycleException e) {
      throw new RuntimeException(e);
    }
  }
}
