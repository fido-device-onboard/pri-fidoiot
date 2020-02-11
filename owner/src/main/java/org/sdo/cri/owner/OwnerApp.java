// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri.owner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import org.sdo.cri.Error;
import org.sdo.cri.KeyType;
import org.sdo.cri.ObjectStorage;
import org.sdo.cri.OwnerService;
import org.sdo.cri.OwnershipVoucher;
import org.sdo.cri.OwnershipVoucherParser;
import org.sdo.cri.ProtocolService;
import org.sdo.cri.RendezvousInfo;
import org.sdo.cri.ServiceInfoModule;
import org.sdo.cri.ServiceInfoMultiSource;
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
public class OwnerApp extends SpringBootServletInitializer implements WebMvcConfigurer {

  private URL myEpidOnlineUrl = null;
  private boolean myIsEpidTestModeEnabled = false;
  private Path myOutputDir = Paths.get(System.getProperty("java.io.tmpdir"));
  private URI myOwnerCertificateUri = null;
  private URI myOwnerKeyUri = null;
  private Path myOwnershipProxyDir = Paths.get(".");
  private List<String> mySecureRandomAlgorithms = List.of("NativePRNG", "Windows-PRNG", "SHA1PRNG");

  // A common point for spring boot config, as we must do it from both main() and configure().
  private static SpringApplicationBuilder configureApplication(SpringApplicationBuilder builder) {
    return builder.sources(OwnerApp.class)
        .bannerMode(Banner.Mode.OFF)
        .properties(getDefaultProperties());
  }

  // Move the default server port, this is an optional convenience
  private static Properties getDefaultProperties() {
    Properties defaults = new Properties();
    defaults.setProperty("server.port", "8042");
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

  // A handler for error messages received from the device.
  // If a device sends an error to msg/255, it'll end up here.
  @Bean
  BiConsumer<OwnershipVoucher, Error> deviceErrorHandler() {
    return (voucher, error) -> {
      logger().error("device " + voucher.getUuid() + " reports error " + error.toString());
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

  // When the protocol service needs to 're-key' the device during TO2 device setup,
  // it needs to generate new credentials with a new g3 and r3.  This hook provides
  // that information when it's needed.
  //
  // This example implementation simply reuses the existing credentials.
  @Bean
  Function<OwnershipVoucher, UUID> g3Function() {
    return (voucher) -> null;
  }

  @Bean
  Function<OwnershipVoucher, RendezvousInfo> r3Function() {
    return (voucher) -> null;
  }

  // The HTTP client to be used by the protocol service for outgoing connections.
  // If we don't have to verify EPID signatures, we don't need this.
  @Bean
  HttpClient httpClient() throws KeyManagementException, NoSuchAlgorithmException {
    return HttpClient.newBuilder().sslContext(sslContext()).build();
  }

  // (Re)initialize the owner service with those transient bits of state
  // which can't be serialized.  We do this when creating a new service
  // and when restoring one from storage.
  private OwnerService initOwnerService(OwnerService ownerService)
      throws NoSuchAlgorithmException,
      KeyManagementException,
      MalformedURLException {

    Function<KeyType, PrivateKey> privateKeyProvider = privateKeyProvider();
    Function<KeyType, PublicKey> publicKeyProvider = publicKeyProvider();

    ownerService.setOwnershipVoucherStorage(ownershipVoucherStorage());
    ownerService.setSecureRandom(secureRandom());
    ownerService.setEpidServiceUrl(epidServiceUrl());
    ownerService.setHttpClient(httpClient());
    ownerService.setKeysProvider(
        type -> new KeyPair(publicKeyProvider.apply(type), privateKeyProvider.apply(type)));
    ownerService.setServiceInfoModules(serviceInfoModules());
    ownerService.setG3Function(g3Function());
    ownerService.setR3Function(r3Function());
    ownerService.setDeviceErrorHandler(deviceErrorHandler());
    return ownerService;
  }

  // All kids love log.
  @Bean
  Logger logger() {
    return LoggerFactory.getLogger(OwnerApp.class);
  }

  // A hook for the OwnerWebController to build protocol services.
  // We don't know which version or service we need until the request URL comes in,
  // so this builder lets the controller ask for the right service when it has information.
  @Bean
  ProtocolServiceBuilder ownerServiceBuilder() {
    return () -> {
      try {
        return initOwnerService(new OwnerService());
      } catch (Exception e) {
        logger().error(e.getMessage(), e);
        return null;
      }
    };
  }

  // The owner service needs to load and store ownership vouchers as part of the
  // transfer ownership process.  We can't provide an input voucher directly
  // because which one we need won't be known until message 40 arrives, and we won't
  // know anything about the output voucher until after message 50.
  @Bean
  ObjectStorage<UUID, OwnershipVoucher> ownershipVoucherStorage() {
    return new ObjectStorage<>() {
      @Override
      public Optional<OwnershipVoucher> load(UUID key) {
        logger().info("searching for SDO voucher " + key + " in " + myOwnershipProxyDir + "...");
        File[] files = myOwnershipProxyDir.toFile().listFiles();
        final OwnershipVoucherParser parser = new OwnershipVoucherParser();
        for (File file : null == files ? new File[0] : files) {

          try (FileReader reader = new FileReader(file)) {
            final OwnershipVoucher op = parser.readObject(reader);
            if (Objects.equals(key, op.getUuid())) {
              logger().info("found voucher " + key + " in " + file);
              return Optional.of(op);
            }
          } catch (IOException e) {
            // If the file doesn't contain a valid op, just ignore it.
          }
        }

        return Optional.empty();
      }

      @Override
      public void store(UUID key, OwnershipVoucher voucher) {
        Path out = myOutputDir.resolve(key + ".op");
        logger().info("storing SDO voucher " + key + " as " + out);
        try (FileWriter w = new FileWriter(out.toFile())) {
          w.write(voucher.toString());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
  }

  // Our private key.
  // This is a simplified (limited) implementation, as true owners need to have a keypair
  // for every key algorithm which their devices might use.  Since this app is configured
  // with a single key it can only respond to one kind of request, and will fail if it has
  // to produce a second type of key.
  //
  // This key is not used inside the protocol library, but is here to provide a hook
  // to our ASYMKEX codec and signature service.
  @Bean
  Function<KeyType, PrivateKey> privateKeyProvider() throws MalformedURLException {

    URL url = null != myOwnerKeyUri ? myOwnerKeyUri.toURL() : null;

    return (keyType) -> {
      if (null != url) {
        try (InputStream instream = url.openStream();
            Reader reader = new InputStreamReader(instream);
            PEMParser pemParser = new PEMParser(reader)) {

          Object o = pemParser.readObject();
          if (o instanceof PrivateKeyInfo) {
            return new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) o);
          } else if (o instanceof PEMKeyPair) {
            return new JcaPEMKeyConverter().getKeyPair((PEMKeyPair) o).getPrivate();
          }
        } catch (IOException e) {
          logger().error(e.getMessage(), e);
        }
      }

      return null;
    };
  }

  // Our public key.  Again, a full implementation would be able to produce public keys
  // of all types which might be requested by a device, but we only have one key.
  @Bean
  Function<KeyType, PublicKey> publicKeyProvider() {
    return (keyType) -> {
      if (null != myOwnerCertificateUri) {
        try {
          URL url = myOwnerCertificateUri.toURL();
          try (InputStream instream = url.openStream()) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate certificate = cf.generateCertificate(instream);
            return certificate.getPublicKey();
          }
        } catch (CertificateException | IOException e) {
          logger().error(e.getMessage(), e);
        }
      }

      return null;
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

        if (o instanceof OwnerService) {
          OwnerService sdoOwnerService = (OwnerService) o;
          try {
            return initOwnerService(sdoOwnerService);
          } catch (Exception e) {
            logger().error(e.getMessage(), e);
            return null;
          }
        } else {
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

  // The registered protocol service info modules.  We install a little example one here
  // in order to show the protocol going through the motions.
  @Bean
  List<ServiceInfoModule> serviceInfoModules() {
    return List.of(new SampleServiceInfoModule());
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

  @Value("${org.sdo.owner.cert}")
  void setOwnerCertificateUri(URI ownerCertificateUri) {
    if (null != ownerCertificateUri) {
      this.myOwnerCertificateUri = toAbsolute(ownerCertificateUri);
    }
  }

  @Value("${org.sdo.owner.key}")
  void setOwnerKeyUri(URI ownerKeyUri) {
    if (null != ownerKeyUri) {
      this.myOwnerKeyUri = toAbsolute(ownerKeyUri);
    }
  }

  @Value("${org.sdo.owner.output-dir:}")
  void setOwnerOutputDir(String dir) {
    if (null != dir && !dir.isBlank()) {
      this.myOutputDir = Paths.get(dir);
    }
  }

  @Value("${org.sdo.owner.proxy-dir:}")
  void setOwnershipProxyDir(String dir) {
    if (null != dir && !dir.isBlank()) {
      this.myOwnershipProxyDir = Paths.get(dir);
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

  // A sample implementation of an owner service info module which says 'hello world'.
  static class SampleServiceInfoModule implements ServiceInfoMultiSource, Serializable {

    @Override
    public List<Entry<CharSequence, CharSequence>> getServiceInfo(UUID id) {
      return List.of();
    }
  }
}
