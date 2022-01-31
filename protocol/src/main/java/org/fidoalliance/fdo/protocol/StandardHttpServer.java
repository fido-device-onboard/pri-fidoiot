package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;


/**
 * Defines a FDO server.
 * <p>
 * FdoSettings and FdoConfig defines settings of the server.
 */
public class StandardHttpServer implements HttpServer {

  private static final LoggerService logger = new LoggerService(StandardHttpServer.class);


  private static class HttpServerConfig  {

    @JsonProperty("base_path")
    private String basePath;
    @JsonProperty("http_port")
    private String httpPort;
    @JsonProperty("https_port")
    private String httpsPort;
    @JsonProperty("http_schemes")
    private String[] httpSchemes;
    @JsonProperty("http_timeout")
    private String timeout;
    @JsonProperty("server_keystore")
    private KeyStoreConfig httpsKeyStore = new KeyStoreConfig();
    @JsonProperty("context_parameters")
    private Map<String, String> additionalParameters = new HashMap<>();

    private String resolve(String value ) {
      return Config.resolve(value);
    }
    public String getBasePath() {
      if (basePath == null || basePath.length() == 0) {
        return Config.getPath();
      }
      return Config.resolvePath(basePath);
    }

    public String getHttpPort() {
      return resolve(httpPort);
    }

    public String getHttpsPort() {
      return resolve(httpsPort);
    }

    public String[] getHttpSchemes() {
      return Config.resolve(httpSchemes);
    }

    public String getTimeout() {
      return resolve(timeout);
    }

    public KeyStoreConfig getHttpsKeyStore() {
      return httpsKeyStore;
    }


    public Map<String, String> getAdditionalParameters() {
      return Config.resolve(additionalParameters);
    }
  }

  private static class HttpRoot {
    @JsonProperty("http-server")
    private HttpServerConfig root;

    public HttpServerConfig getRoot() { return root; }
  }

  private HttpServerConfig config = Config.getConfig(HttpRoot.class).getRoot();

  @Override
  public void run() {


    Tomcat tomcat = new Tomcat();

    tomcat.setBaseDir(config.getBasePath());
    tomcat.setAddDefaultWebXmlToWebapp(false);

    StandardSessionIdGenerator std = new StandardSessionIdGenerator();

    Context ctx = tomcat.addWebapp("", config.getBasePath());

    //tomcat startup (disable jar scanning)
    if (ctx.getJarScanner() instanceof StandardJarScanner) {
      ((StandardJarScanner) ctx.getJarScanner()).setScanManifest(false);
    }


    //long theadId = Thread.currentThread().getId();

    //set context manager

    //set randome number gennerator

    //String  sid = sig.generateSessionId();
    //ctx.getManager().setSessionIdGenerator();

    ///Context ctx = tomcat.addWebapp("", "./demo");
    //ctx.setParentClassLoader(ctx.getClass().getClassLoader());

    /*try {
      File file = new File("context.xml");
      ctx.setConfigFile(file.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }*/

    Service service = tomcat.getService();
    //service.addExecutor(new StandardThreadExecutor());

    String[] schemes = config.getHttpSchemes();

    for (String scheme : schemes) {

      if (scheme.toLowerCase().equals("https")) {
        Connector httpsConnector = new Connector();
        int httpsPort = Integer.parseInt(config.getHttpsPort());
        httpsConnector.setPort(httpsPort);
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");

        //keystoreFile
        //This is an alias for the certificateKeystoreFile attribute of the first Certificate element nested in the SSLHostConfig element with the hostName of _default_. If this Certificate and/or SSLHostConfig element is not explicitly defined, they will be created.
        //
        //keystorePass
        //This is an alias for the certificateKeystorePassword attribute of the first Certificate element nested in the SSLHostConfig element with the hostName of _default_. If this Certificate and/or SSLHostConfig element is not explicitly defined, they will be created.
        //
        //keystoreProvider
        //This is an alias for the certificateKeystoreProvider attribute of the first Certificate element nested in the SSLHostConfig element with the hostName of _default_. If this Certificate and/or SSLHostConfig element is not explicitly defined, they will be created.
        //
        //keystoreType
        //This is an alias for the certificateKeystoreType attribute of the first Certificate element nested in the SSLHostConfig element with the hostName of _default_. If this Certificate and/or SSLHostConfig element is not explicitly defined, they will be created.
        httpsConnector.setProperty("keystorePass", config.getHttpsKeyStore().getPassword());
        httpsConnector.setProperty("keystoreFile", config.getHttpsKeyStore().getPath());
        httpsConnector.setProperty("clientAuth", "false");
        httpsConnector.setProperty("sslProtocol", "TLS");
        httpsConnector.setProperty("SSLEnabled", "true");
        httpsConnector.setProperty("connectionTimeout",
            config.getTimeout());
        service.addConnector(httpsConnector);

      } else if (scheme.toLowerCase().equals("http")) {
        Connector httpsConnector = new Connector();
        int httpPort = Integer.parseInt(config.getHttpPort());

        httpsConnector.setPort(httpPort);
        httpsConnector.setScheme("http");
        httpsConnector.setProperty("protocol", "HTTP/1.1");
        httpsConnector.setProperty("connectionTimeout",
            config.getTimeout());
        service.addConnector(httpsConnector);
        tomcat.setConnector(httpsConnector);

      }
    }


    StandardManager stdManager = new StandardManager();
    ctx.setManager(stdManager);
    try {
      SecureRandom random = SecureRandom.getInstanceStrong();
      String alg = random.getAlgorithm();

      std.setSecureRandomAlgorithm(random.getAlgorithm());

      stdManager.setSecureRandomAlgorithm(random.getAlgorithm());
      stdManager.setSessionIdGenerator(std);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    tomcat.getConnector();

    try {

      tomcat.start();

      logger.info("Started All-in-One Demo Service.");
    } catch (LifecycleException e) {
      logger.warn("Failed to start All-in-One Demo Service.");
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getPort() {
    return config.getHttpPort();
  }

}

