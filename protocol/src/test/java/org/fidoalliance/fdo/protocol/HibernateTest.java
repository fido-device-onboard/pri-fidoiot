package org.fidoalliance.fdo.protocol;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;


public class HibernateTest {

  @Test
  public void Test() throws DecoderException, IOException {
    /*
    System.out.println(System.getProperty("user.dir"));
    List<String> tcpParams = new ArrayList<>();
    tcpParams.add("-ifNotExists");



    String[] tcpArgs = new String[tcpParams.size()];
    tcpParams.toArray(tcpArgs);
    int port = DEFAULT_PORT;
    //Server server = null;

    // variables
    //String dbURL = "jdbc:derby:testdb;create=true;user=me;password=mine";
    String dbURL = "jdbc:derby:testdb";

    Connection con = null;

    //con = DriverManager.getConnection(dbURL);
    //con.close();
    hibernate();


      Class.forName("org.h2.Driver");

      server = Server.createTcpServer(tcpArgs);
      server.start();
      port = server.getPort();
      boolean running = server.isRunning(true);
      if (running) {

        List<String> webParams = new ArrayList<>();

        webParams.add("-web");
        webParams.add("-webAllowOthers");
        webParams.add("-webPort");
        webParams.add("8082");

        String[] webArgs = new String[webParams.size()];
        webArgs = webParams.toArray(webArgs);
        Server web = Server.createWebServer(webArgs);
        web.start();
        int webPort = web.getPort();
        if (web.isRunning(true)) {
          System.out.println("Web server running at " + webPort);
          String connectionString = "jdbc:h2:tcp://localhost:"
              + port + "/./" + dbName;

          System.out.println(connectionString);
          hibernate();
          Thread.currentThread().join();
          HibernateUtil.shutdown();

        }

      }*/

  }


}
