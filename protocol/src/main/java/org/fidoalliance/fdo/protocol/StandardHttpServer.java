package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;


/**
 * Defines a FDO server.
 * <p>
 * FdoSettings and FdoConfig defines settings of the server.
 */
public class StandardHttpServer implements HttpServer {

  private static final LoggerService logger = new LoggerService(StandardHttpServer.class);

  private static class AuthConfig {

    @JsonProperty("method")
    private String method;
    @JsonProperty("role")
    private String role;
    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("user_password")
    private String password;

    private String resolve(String value) {
      return Config.resolve(value);
    }

    public String getMethod() {
      return resolve(method);
    }

    public String getRole() {
      return resolve(role);
    }

    public String getUserName() {
      return resolve(userName);
    }

    public String getPassword() {
      return resolve(password);
    }
  }

  private static class HttpServerConfig {

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
    @JsonProperty("auth")
    private AuthConfig authConfig;
    @JsonProperty("keystore")
    private KeyStoreConfig httpsKeyStore;
    @JsonProperty("context_parameters")
    private Map<String, String> additionalParameters = new HashMap<>();

    private String resolve(String value) {
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

    public AuthConfig getAuthConfig() {
      return authConfig;
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

    public HttpServerConfig getRoot() {
      return root;
    }
  }

  private HttpServerConfig config = Config.getConfig(HttpRoot.class).getRoot();

  @Override
  public void run() {

    Tomcat tomcat = new Tomcat();

    tomcat.setBaseDir(config.getBasePath());

    tomcat.setAddDefaultWebXmlToWebapp(false);

    StandardSessionIdGenerator std = new StandardSessionIdGenerator();

    Context ctx = tomcat.addWebapp("", Config.getPath());

    //tomcat startup (disable jar scanning)
    if (ctx.getJarScanner() instanceof StandardJarScanner) {
      ((StandardJarScanner) ctx.getJarScanner()).setScanManifest(false);
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

    if (config.getAuthConfig() != null) {
      AuthConfig authConfig = config.getAuthConfig();
      tomcat.addRole(authConfig.getUserName(), authConfig.getRole());
      tomcat.addUser(authConfig.getUserName(), authConfig.getPassword());
      LoginConfig loginConfig = new LoginConfig();
      loginConfig.setAuthMethod(authConfig.getMethod());
      ctx.setLoginConfig(loginConfig);
    }

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
        SSLHostConfig sslHostConfig = null;

        KeyStoreConfig storeConfig = config.getHttpsKeyStore();

        File sslFile = new File(config.getHttpsKeyStore().getPath());
        if (sslFile.exists()) {
          KeyStoreConfig keyStoreConfig = config.getHttpsKeyStore();

          try (InputStream input = new FileInputStream(sslFile)) {
            KeyStore ks = KeyStore.getInstance(keyStoreConfig.getStoreType());
            ks.load(input, keyStoreConfig.getPassword().toCharArray());

            SSLHostConfigCertificate certConfig = null;
            Certificate cert = ks.getCertificate(keyStoreConfig.getAlias());
            if (cert != null) {
              PublicKey publicKey = cert.getPublicKey();
              if (publicKey instanceof RSAKey) {
                sslHostConfig = new SSLHostConfig();
                certConfig = new SSLHostConfigCertificate(
                    sslHostConfig, SSLHostConfigCertificate.Type.RSA);
              } else if (publicKey instanceof ECPublicKey) {
                sslHostConfig = new SSLHostConfig();
                certConfig = new SSLHostConfigCertificate(
                    sslHostConfig, Type.EC);
              }
            }
            if (certConfig != null) {
              certConfig.setCertificateKeystore(ks);
              certConfig.setCertificateKeyAlias(keyStoreConfig.getAlias());
              certConfig.setCertificateKeyPassword(keyStoreConfig.getPassword());
              sslHostConfig.addCertificate(certConfig);
              httpsConnector.addSslHostConfig(sslHostConfig);
            }
          } catch (Exception e) {
            logger.error(e.getMessage());
          }
        }

        httpsConnector.setProperty("clientAuth", "false");
        httpsConnector.setProperty("sslProtocol", "TLS");
        httpsConnector.setProperty("SSLEnabled", "true");
        httpsConnector.setProperty("connectionTimeout", config.getTimeout());

        if (sslHostConfig != null) {
          service.addConnector(httpsConnector);
        } else {
          logger.warn("SSL not configured - Failed to create keystore");
        }

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

