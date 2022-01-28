package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.h2.tools.Server;


public class StandardDatabaseServer implements DatabaseServer {

  private static LoggerService logger = new LoggerService(DatabaseServer.class);

  @Override
  public void start() throws IOException {

    //todo: read config yml
    //tcp port should be 9092
    //tcp port should be 8082
    List<String> list = new ArrayList<>();
    list.add("-ifNotExists");
    try {
      //get the port from the connection string
      Server server = Server.createTcpServer(list.toArray(String[]::new)).start();

      logger.info("database tcp port " + server.getPort());
      list.clear();
      list.add("-webAllowOthers");
      list.add("-webExternalNames");
      list.add("fdo10.westus.cloudapp.azure.com");
      list.add("-ifNotExists");
      Server web = Server.createWebServer(list.toArray(String[]::new)).start();
      logger.info("database web port " + web.getPort());
    } catch (SQLException e) {
      throw new IOException(e);
    }
    //   "-tcpPort", "9123", "-tcpAllowOthers").start();
    //Supported options are: -tcpPort, -tcpSSL, -tcpPassword, -tcpAllowOthers, -tcpDaemon, -trace, -ifExists, -ifNotExists, -baseDir, -key. See the main method for details.

  }
}
