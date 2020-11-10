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
      .loadConfig(OwnerAppConstants.TO2_PORT)
          ? Integer.parseInt(OwnerConfigLoader.loadConfig(OwnerAppConstants.TO2_PORT))
          : 8042;

  private static String getMessagePath(int msgId) {
    return OwnerAppConstants.WEB_PATH + "/" + Integer.toString(msgId);
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
    System.setProperty(OwnerAppConstants.SERVER_PATH,
        Path.of(OwnerConfigLoader.loadConfig(OwnerAppConstants.SERVER_PATH)).toAbsolutePath()
            .toString());
    
    Context ctx = tomcat.addContext("", null);
    
    ctx.addParameter(OwnerAppConstants.DB_URL,
        OwnerConfigLoader.loadConfig(OwnerAppConstants.DB_URL));
    ctx.addParameter(OwnerAppConstants.DB_USER,
        OwnerConfigLoader.loadConfig(OwnerAppConstants.DB_USER));
    ctx.addParameter(OwnerAppConstants.DB_PWD,
        OwnerConfigLoader.loadConfig(OwnerAppConstants.DB_PWD));
    // hard-coded H2 config
    ctx.addParameter("db.tcpServer", "-tcp -tcpAllowOthers -ifNotExists -tcpPort "
        + OwnerConfigLoader.loadConfig(OwnerAppConstants.DB_PORT));
    ctx.addParameter("webAllowOthers", "true");
    ctx.addParameter("trace", "");
    
    ctx.addParameter(OwnerAppConstants.OWNER_KEYSTORE_PWD,
        OwnerConfigLoader.loadConfig(OwnerAppConstants.OWNER_KEYSTORE_PWD));
    ctx.addParameter(OwnerAppConstants.TO0_SCHEDULING_ENABLED,
        OwnerConfigLoader.loadConfig(OwnerAppConstants.TO0_SCHEDULING_ENABLED));
    ctx.addParameter(OwnerAppConstants.TO0_SCHEDULING_INTREVAL,
        OwnerConfigLoader.loadConfig(OwnerAppConstants.TO0_SCHEDULING_INTREVAL));
    ctx.addParameter(OwnerAppConstants.TO0_RV_BLOB,
        OwnerConfigLoader.loadConfig(OwnerAppConstants.TO0_RV_BLOB));
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
