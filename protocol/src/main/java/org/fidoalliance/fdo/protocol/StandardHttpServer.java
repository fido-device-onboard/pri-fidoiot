// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.catalina.Context;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;

/**
 * Defines a FDO server.
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
    @JsonProperty("address")
    private String address;
    @JsonProperty("truststore_file")
    private String trustStoreFile;
    @JsonProperty("truststore_type")
    private String trustStoreType;
    @JsonProperty("truststore_password")
    private String trustStorePassword;
    @JsonProperty("server_cert")
    private String serverCert;
    @JsonProperty("server_key")
    private String serverKey;
    @JsonProperty("protocols")
    private String protocols;
    @JsonProperty("ciphers")
    private String ciphers;
    @JsonProperty("certificate_verification")
    private String certVerification;
    @JsonProperty("certificate_verification_depth")
    private String certVerificationDepth;
    @JsonProperty("http_schemes")

    private String[] httpSchemes;
    @JsonProperty("http_timeout")
    private String timeout;
    @JsonProperty("context_parameters")
    private final Map<String, String> additionalParameters = new HashMap<>();

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

    public String getAddress() {
      return resolve(address);
    }

    public String getTrustStoreFile() {
      return resolve(trustStoreFile);
    }

    public String getTrustStoreType() {
      return resolve(trustStoreType);
    }

    public String getTrustStorePassword() {
      return resolve(trustStorePassword);
    }

    public String getServerCert() {
      return resolve(serverCert);
    }

    public String getServerKey() {
      return resolve(serverKey);
    }


    public String getProtocols() {
      return resolve(protocols);
    }

    public String getCiphers() {
      return resolve(ciphers);
    }

    public String[] getHttpSchemes() {
      return Config.resolve(httpSchemes);
    }

    public String getTimeout() {
      return resolve(timeout);
    }

    public String getCertificateVerification() {
      return resolve(certVerification);
    }

    public String getCertificateVerificationDepth() {
      return resolve(certVerificationDepth);
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

  private final HttpServerConfig config = Config.getConfig(HttpRoot.class).getRoot();

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
    try {
      if (Paths.get("context.xml").toFile().exists()) {
        ctx.setConfigFile(Paths.get("context.xml").toUri().toURL());
      } else {
        ctx.setConfigFile(Paths.get(Config.getPath(),"context.xml").toUri().toURL());
      }
      SecureRandom random = SecureRandom.getInstanceStrong();
      String alg = random.getAlgorithm();

      std.setSecureRandomClass(random.getClass().getName());
      std.setSecureRandomAlgorithm(alg);
      stdManager.setSessionIdGenerator(std);
      stdManager.setSecureRandomAlgorithm(alg);
      stdManager.setSecureRandomClass(random.getClass().getName());

    } catch (NoSuchAlgorithmException | MalformedURLException e) {
      throw new RuntimeException(e);
    }
    ctx.setManager(stdManager);

    Service service = tomcat.getService();

    String[] schemes = config.getHttpSchemes();
    String ipAddress = config.getAddress();

    for (String scheme : schemes) {

      if (scheme.equalsIgnoreCase(HttpUtils.HTTPS_SCHEME)) {
        Connector httpsConnector = new Connector();
        if (ipAddress != null) {
          httpsConnector.setProperty("address", ipAddress);
        }
        try {
          httpsConnector.setPort(Integer.parseInt(config.getHttpsPort()));
        } catch (NumberFormatException e) {
          logger.error("Invalid HTTPS port.");
          throw new RuntimeException(e);
        }
        httpsConnector.setSecure(true);
        httpsConnector.setEnableLookups(true);

        httpsConnector.setScheme(HttpUtils.HTTPS_SCHEME);
        httpsConnector.addUpgradeProtocol(new Http2Protocol());
        SSLHostConfig sslHostConfig = null;

        try {
          KeyStore ks = loadKeystore();
          SSLHostConfigCertificate certConfig = null;

          Certificate cert = ks.getCertificate("0");

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
            certConfig.setCertificateKeyPassword("");
            certConfig.setCertificateKeyAlias("0");
            sslHostConfig.addCertificate(certConfig);

            sslHostConfig.setTruststoreFile(config.getTrustStoreFile());
            sslHostConfig.setTruststoreType(config.getTrustStoreType());
            sslHostConfig.setTruststorePassword(config.getTrustStorePassword());

            sslHostConfig.setCertificateVerificationDepth(
                Integer.parseInt(config.getCertificateVerificationDepth()));
            sslHostConfig.setCertificateVerification(config.getCertificateVerification());
            if (config.ciphers != null) {
              sslHostConfig.setCiphers(config.getCiphers());
            }
            if (config.protocols != null) {
              sslHostConfig.setSslProtocol(config.getProtocols());
            }

            httpsConnector.addSslHostConfig(sslHostConfig);
          }
        } catch (Exception e) {

          logger.error(e.getMessage());
        }

        httpsConnector.setProperty("sslProtocol", "TLS");
        httpsConnector.setProperty("SSLEnabled", "true");
        httpsConnector.setProperty("connectionTimeout", config.getTimeout());

        if (sslHostConfig != null) {
          service.addConnector(httpsConnector);
        } else {
          logger.warn("SSL not configured - Failed to create keystore");
        }

      } else if (scheme.equalsIgnoreCase(HttpUtils.HTTP_SCHEME)) {
        Connector httpsConnector = new Connector();

        if (ipAddress != null) {
          httpsConnector.setProperty("address", ipAddress);
        }
        try {
          httpsConnector.setPort(Integer.parseInt(config.getHttpPort()));
        } catch (NumberFormatException e) {
          logger.error("Invalid HTTP port");
          throw new RuntimeException(e);
        }

        httpsConnector.setScheme(HttpUtils.HTTP_SCHEME);
        httpsConnector.setProperty("protocol", "HTTP/1.1");
        httpsConnector.setProperty("connectionTimeout",
            config.getTimeout());
        service.addConnector(httpsConnector);
        tomcat.setConnector(httpsConnector);

      }
    }

    tomcat.getConnector();

    String serviceName = System.getProperty("service.name");
    try {
      tomcat.start();
      logger.info("Started " + serviceName + " Service.");
    } catch (Throwable e) {
      logger.warn("Failed to start " + serviceName + " Service.");
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getHttpPort() {
    return config.getHttpPort();
  }

  @Override
  public String getHttpsPort() {
    return config.getHttpsPort();
  }


  protected KeyStore loadKeystore() throws IOException {

    PrivateKey privateKey = null;
    try {
      KeyStore ks = KeyStore.getInstance("PEM");
      ks.load(null,"".toCharArray());

      String pemString = Files.readString(Path.of(config.getServerKey()));
      privateKey = PemLoader.loadPrivateKey(pemString,"");
      pemString = Files.readString(Path.of(config.getServerCert()));
      List<Certificate> certs = PemLoader.loadCerts(pemString);

      ks.setKeyEntry("0",privateKey,"".toCharArray(),
          certs.stream().toArray(Certificate[]::new));

      return ks;
    } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
      throw new IOException(e);
    } finally {
      Config.getWorker(CryptoService.class).destroyKey(privateKey);
    }
  }


}

