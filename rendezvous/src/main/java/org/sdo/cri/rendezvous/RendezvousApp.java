// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri.rendezvous;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.sdo.cri.ObjectStorage;
import org.sdo.cri.OwnershipVoucher;
import org.sdo.cri.PerishableRecord;
import org.sdo.cri.ProtocolService;
import org.sdo.cri.RendezvousDeviceService;
import org.sdo.cri.RendezvousOwnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class RendezvousApp extends SpringBootServletInitializer implements WebMvcConfigurer {

  private URL myEpidOnlineUrl = null;
  private boolean myIsEpidTestModeEnabled = false;
  private List<String> mySecureRandomAlgorithms = List.of("NativePRNG", "Windows-PRNG", "SHA1PRNG");

  // A common point for spring boot config, as we must do it from both main() and configure().
  private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder) {
    return builder.sources(RendezvousApp.class)
        .bannerMode(Banner.Mode.OFF)
        .properties(getDefaultProperties());
  }

  // Move the default server port, this is an optional convenience
  private static Properties getDefaultProperties() {
    Properties defaults = new Properties();
    defaults.setProperty("server.port", "8040");
    return defaults;
  }

  /**
   * Command-line entry point.
   */
  public static void main(String[] args) {
    configureApplication(new SpringApplicationBuilder()).run(args);
  }

  // Some URI actions require absolute URIs, so this provides a common mechanism
  // for making them absolute.
  private static URI toAbsolute(URI uri) {
    URI pwd = Paths.get(".").toUri();
    URI resolved = pwd.resolve(uri);
    return resolved.normalize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return configureApplication(builder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    // Rather than failing hard because a data type has no message converter,
    // make an attempt to pass useful information out by representing unexpected
    // types as strings.
    HttpMessageConverter<Object> fallbackMessageConverter = new HttpMessageConverter<>() {

      @Override
      public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
      }

      @Override
      public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return true;
      }

      @Override
      public List<MediaType> getSupportedMediaTypes() {
        return List.of(MediaType.TEXT_PLAIN);
      }

      @Override
      public Object read(Class<?> clazz, HttpInputMessage httpInputMessage)
          throws HttpMessageNotReadableException {
        return null;
      }

      @Override
      public void write(Object o, MediaType mediaType, HttpOutputMessage httpOutputMessage)
          throws IOException, HttpMessageNotWritableException {

        httpOutputMessage.getBody().write(o.toString().getBytes());
      }
    };

    converters.add(fallbackMessageConverter);
  }

  // A trivial in-RAM redirection map for this example.  Real services will want more than this.
  @Bean
  ObjectStorage<UUID, PerishableRecord> redirectionMap() {

    return new ObjectStorage<>() {
      Map<UUID, PerishableRecord> myMap = new HashMap<>();

      @Override
      public Optional<PerishableRecord> load(UUID key) {
        myMap.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return Optional.ofNullable(myMap.get(key));
      }

      @Override
      public void store(UUID key, PerishableRecord value) {
        myMap.put(key, value);
      }
    };
  }

  // Configure EPID service options via the same two-property scheme used in SDO <= 1.6
  @Bean
  URL epidServiceUrl() throws MalformedURLException {
    // Algorithm was chosen to preserve legacy behavior.
    //
    // Effective URL is:
    //
    //                  epidOnlineUrl
    //                  set            | not set
    //                 ----------------+---------
    // testMode  true  | epidOnlineUrl | sandbox EPID service
    //           false | epidOnlineUrl | production EPID service
    if (null != myEpidOnlineUrl) {
      return myEpidOnlineUrl;
    } else if (myIsEpidTestModeEnabled) {
      logger().warn("***");
      logger().warn("Epid test use has been enabled.");
      logger().warn("This should only be used in test and development environments.");
      logger().warn("If this is a production environment then you should update");
      logger().warn("the configuration to set test mode to false.");
      logger().warn("***");
      return new URL("https://verify.epid-sbx.trustedservices.intel.com");
    } else {
      return new URL("https://verify.epid.trustedservices.intel.com");
    }
  }

  // The web controller needs an executor in order to run asynchronously
  @Bean
  ExecutorService executorService() {
    return Executors.newCachedThreadPool();
  }

  // The HTTP client to be used by the protocol service for outgoing connections.
  // If we don't have to verify EPID signatures, we don't need this.
  @Bean
  HttpClient httpClient() throws KeyManagementException, NoSuchAlgorithmException {
    return HttpClient.newBuilder().sslContext(sslContext()).build();
  }

  private ProtocolService initRendezvousService(RendezvousDeviceService service)
      throws MalformedURLException, NoSuchAlgorithmException, KeyManagementException {

    service.setSecureRandom(secureRandom());
    service.setEpidServiceUrl(epidServiceUrl());
    service.setHttpClient(httpClient());
    service.setRedirectionMap(redirectionMap());
    return service;
  }

  private ProtocolService initRendezvousService(RendezvousOwnerService service) {
    service.setSecureRandom(secureRandom());
    service.setWaitSecondsResponder(waitSecondsResponder());
    service.setRedirectionMap(redirectionMap());
    return service;
  }

  // All kids love log.
  @Bean
  Logger logger() {
    return LoggerFactory.getLogger(RendezvousApp.class);
  }

  // Hooks for building protocol services.
  // We don't know which version or service we need until the request URL comes in,
  // so this builder lets the controller ask for the right service when it has information.
  @Bean
  ProtocolServiceBuilder rendezvousDeviceServiceBuilder() {
    return () -> {
      try {
        return initRendezvousService(new RendezvousDeviceService());
      } catch (Exception e) {
        logger().error(e.getMessage(), e);
        return null;
      }
    };
  }

  @Bean
  ProtocolServiceBuilder rendezvousOwnerServiceBuilder() {
    return () -> {
      try {
        return initRendezvousService(new RendezvousOwnerService());
      } catch (Exception e) {
        logger().error(e.getMessage(), e);
        return null;
      }
    };
  }

  // A storage service for protocol service objects.  Between requests, the web service
  // has to put the protocol objects somewhere, and this provides those hooks.
  @Bean
  ProtocolServiceStorage sdoServiceStorage() {
    return new ProtocolServiceStorage() {

      private final Map<UUID, byte[]> myMap = new HashMap<>();

      @Override
      public synchronized UUID put(ProtocolService protocolService) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ObjectOutputStream(out).writeObject(protocolService);
        UUID sessionId = UUID.randomUUID();
        byte[] bytes = out.toByteArray();
        myMap.put(sessionId, Arrays.copyOf(bytes, bytes.length));
        return sessionId;
      }

      @Override
      public synchronized ProtocolService take(UUID sessionId) {

        byte[] bytes = myMap.remove(sessionId);
        if (null == bytes) {
          return null;
        }

        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        final Object o;
        try {
          o = new ObjectInputStream(in).readObject();
        } catch (Exception e) {
          return null;
        }

        try {
          if (o instanceof RendezvousDeviceService) {
            RendezvousDeviceService service = (RendezvousDeviceService) o;
            return initRendezvousService(service);
          } else if (o instanceof RendezvousOwnerService) {
            RendezvousOwnerService service = (RendezvousOwnerService) o;
            return initRendezvousService(service);
          } else {
            return null;
          }
        } catch (Exception e) {
          logger().error(e.getMessage(), e);
          return null;
        }
      }
    };
  }

  // The source of our randomness.
  @Bean
  SecureRandom secureRandom() {
    for (String algo : mySecureRandomAlgorithms) {

      // Legacy implementations used an enum here, which couldn't have hyphens in names.
      // Since some SecureRandom algorithms are hyphenated (Windows-PRNG), swap underscores
      // for hyphens to enable those legacy names.
      algo = algo.replaceAll("_", "-");
      try {
        SecureRandom secureRandom = SecureRandom.getInstance(algo);
        logger().info("using SecureRandom " + secureRandom.getAlgorithm());
        return secureRandom;

      } catch (NoSuchAlgorithmException e) {
        // provider not available?  just move on to the next
        logger().info("SecureRandom " + algo + " is not available");
      }
    }

    return null;
  }

  @Value("${org.sdo.epid.test-mode:false}")
  void setEnableEpidTestMode(boolean isEpidTestModeEnabled) {
    this.myIsEpidTestModeEnabled = isEpidTestModeEnabled;
  }

  @Value("${org.sdo.epid.epid-online-url:}")
  void setEpidOnlineUrl(String s) throws MalformedURLException {
    if (null != s && !s.isBlank()) {
      this.myEpidOnlineUrl = new URL(s);
    }
  }

  @Value("${org.sdo.secure-random:}")
  void setSecureRandomAlgorithms(@Nullable List<String> secureRandomAlgorithms) {
    if (null != secureRandomAlgorithms && !secureRandomAlgorithms.isEmpty()) {
      this.mySecureRandomAlgorithms = secureRandomAlgorithms;
    }
  }

  // The SSLContext for our HttpClient.
  // If we're not validating EPID signatures, we don't need this.
  // This simple example doesn't check root of trust so it'll work against self-signed certificates.
  // Real implementations shouldn't do this.
  @Bean
  SSLContext sslContext() throws NoSuchAlgorithmException, KeyManagementException {

    TrustManager[] trustManagers = new TrustManager[]{
        new X509ExtendedTrustManager() {
          @Override
          public void checkClientTrusted(X509Certificate[] chain, String authType, Socket s) {
          }

          @Override
          public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine e) {
          }

          @Override
          public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
          }

          @Override
          public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
          }

          @Override
          public void checkServerTrusted(X509Certificate[] chain, String authType, Socket s) {
          }

          @Override
          public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine e) {
          }

          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }
        }
    };

    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, trustManagers, secureRandom());
    LoggerFactory.getLogger(getClass()).warn("UNSAFE: no-op TrustManager installed");

    return context;
  }

  // The rendezvous service calls this responder to decide how to respond
  // to TO0.OwnerSign.to0d.ws
  @Bean
  BiFunction<OwnershipVoucher, Duration, Duration> waitSecondsResponder() {
    return (voucher, request) -> request;
  }
}
