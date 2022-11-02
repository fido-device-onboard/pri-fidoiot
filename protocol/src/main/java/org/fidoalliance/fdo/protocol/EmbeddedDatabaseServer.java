// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import org.h2.tools.Server;


public class EmbeddedDatabaseServer implements DatabaseServer, Closeable {


  private static final LoggerService logger = new LoggerService(DatabaseServer.class);



  private static class RootConfig {
    @JsonProperty("h2-database")
    private ServiceConfig config = new ServiceConfig();

    public ServiceConfig getConfig() {
      return config;
    }
  }

  private static class ServiceConfig {
    @JsonProperty("web-server")
    private String[] webArgs;

    @JsonProperty("tcp-server")
    private String[] tcpArgs = new String[0];

    public String[] getTcpArgs() {
      return Config.resolve(tcpArgs);
    }

    public String[] getWebArgs() {
      if (webArgs != null) {
        return Config.resolve(webArgs);
      }
      return null;
    }
  }



  private static final ServiceConfig config = Config.getConfig(RootConfig.class).getConfig();


  private Server webServer;
  private Server tcpServer;

  @Override
  public void start() throws IOException {

    //-tcpPort, -tcpSSL, -tcpPassword, -tcpAllowOthers, -tcpDaemon, -trace, -ifExists,
    // -ifNotExists, -baseDir, -key
    //-webPort, -webSSL, -webAllowOthers, -webDaemon, -trace, -ifExists,
    // -ifNotExists, -baseDir,-properties

    //tcp port should be 9092
    //tcp port should be 8082

    try {
      //get the port from the connection string
      tcpServer = Server.createTcpServer(config.getTcpArgs()).start();

      logger.info("database tcp port " + tcpServer.getPort());

      String[] webArgs = config.getWebArgs();
      if (webArgs != null) {
        webServer = Server.createWebServer(webArgs).start();
        logger.info("database web port " + webServer.getPort());
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }

  }

  @Override
  public void close() throws IOException {
    if (webServer != null && webServer.isRunning(true)) {
      webServer.stop();
      webServer = null;
    }

    if (tcpServer != null && tcpServer.isRunning(true)) {
      tcpServer.stop();
      tcpServer = null;
    }

  }

}