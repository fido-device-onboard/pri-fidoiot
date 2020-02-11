// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri.device;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
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
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import org.sdo.cri.CipherBlockMode;
import org.sdo.cri.DeviceCredentials;
import org.sdo.cri.DeviceInitializationClient;
import org.sdo.cri.DeviceTransferOwnershipClient;
import org.sdo.cri.KeyType;
import org.sdo.cri.ProtocolException;
import org.sdo.cri.SdoDevModuleDevice;
import org.sdo.cri.SdoSysModuleDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;

// A Java-based SDO Device application.
@Lazy
@SpringBootApplication
public class DeviceApp {

  static final String PROPERTY_CIPHER_BLOCK_MODE = "org.sdo.cipher-block-mode";
  static final String PROPERTY_DI_URI = "org.sdo.di.uri";
  static final String PROPERTY_DEVICE_MODEL = "org.sdo.pm.credmfg.d";
  static final String PROPERTY_DEVICE_OUTPUT_DIR = "org.sdo.device.output-dir";
  static final String PROPERTY_SECURE_RANDOM_ALGORITHMS = "org.sdo.secure-random";
  static final String PROPERTY_DEVICE_SERIAL_NUMBER = "org.sdo.device.serial";
  static final String PROPERTY_STOP_AFTER_DI = "org.sdo.device.stop-after-di";
  static final String PROPERTY_DEVICE_CERT = "org.sdo.device.cert";
  static final String PROPERTY_DEVICE_CREDENTIALS = "org.sdo.device.credentials";
  static final String PROPERTY_DEVICE_KEY = "org.sdo.device.key";
  private CipherBlockMode cipherBlockMode = CipherBlockMode.CTR;
  private URI deviceCertificateUri = null;
  private URI deviceCredentialsUri = null;
  private URI deviceKeyUri = null;
  private boolean isStopAfterDi = true;
  private URI mfrUri = null;
  private String model = "SDO Java Device";
  private Path outputDir = null;
  private List<String> secureRandomAlgorithms = List.of("NativePRNG", "Windows-PRNG", "SHA1PRNG");
  private String serial = buildDefaultSerial();

  /**
   * The application entry point.
   *
   * @param args The command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(DeviceApp.class);
    app.setBannerMode(Banner.Mode.OFF);
    app.setWebApplicationType(WebApplicationType.NONE);
    System.exit(SpringApplication.exit(app.run(args)));
  }

  private static URI toAbsolute(URI uri) {
    URI pwd = Paths.get(".").toUri();
    URI resolved = pwd.resolve(uri);
    return resolved.normalize();
  }

  // The ApplicationRunner provides the main(argv) entry point to Spring.
  // The usual string-array argv is replaced with an ApplicationArguments.
  @Bean
  ApplicationRunner applicationRunner() {
    return args -> {
      // If we have no credentials, we must run DI to get some.
      DeviceCredentials dc = deviceCredentialsStorage().load();
      if (null == dc) {

        logger().info("device initialization begins");
        final DeviceInitializationClient di = sdoDeviceInit();
        dc = di.call();
        if (null != dc) {
          deviceCredentialsStorage().store(dc);
          logger().info("device initialization ends");

        } else {
          throw new RuntimeException("device initialization failure");
        }

        if (isStopAfterDi) {
          return;
        }
      }

      logger().info("device onboarding begins");
      final DeviceTransferOwnershipClient to = sdoDeviceOnboardFactory().get();
      final Optional<DeviceCredentials> newDc = to.call();
      if (null != newDc) {
        newDc.ifPresent(c -> {
          try {
            deviceCredentialsStorage().store(c);
          } catch (IOException e) {
            logger().error(e.getMessage(), e);
          }
        });
        logger().info("device onboarding ends");
      } else {
        throw new RuntimeException("device onboarding failure");
      }
    };
  }

  // Build a unique-ish serial number for this device.
  //
  // If a serial number is provided via properties, prefer that.
  // We can't have a blank serial number, so if one isn't provided
  // build something constant-ish and unique-ish out of the MAC addresses of this host.
  private String buildDefaultSerial() {

    SHA256Digest digest = new SHA256Digest();
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      Iterator<NetworkInterface> it = interfaces.asIterator();
      while (it.hasNext()) {
        NetworkInterface networkInterface = it.next();
        byte[] addr = networkInterface.getHardwareAddress();
        if (null != addr) {
          digest.update(addr, 0, addr.length);
        }
      }
    } catch (SocketException e) {
      // no-op
    }

    StringBuilder stringBuilder = new StringBuilder();
    byte[] result = new byte[digest.getDigestSize()];
    digest.doFinal(result, 0);
    for (byte b : result) {
      stringBuilder.append(Integer.toUnsignedString(Byte.toUnsignedInt(b), 16));
    }
    return stringBuilder.toString();
  }

  // Storage for device credentials.  We defer to a DeviceCredentials object
  // decisions about where and how to store them.
  @Bean
  DeviceCredentialsStorage deviceCredentialsStorage() {
    return new DeviceCredentialsStorage(deviceCredentialsUri, outputDir);
  }

  // The device's 'manufacturing mark', or 'm'.
  // This gets used in the DI protocol to inform the manufacturer of lots of stuff, including:
  // - what kind of owner key we can parse,
  // - our serial and model numbers,
  // - our CSR
  @Bean
  String deviceMark() throws CertificateException, IOException, OperatorCreationException {
    StringBuilder m = new StringBuilder();
    String csr = null;

    PublicKey myPublicKey = devicePublicKey();

    KeyType myKeyType = KeyType.ofKey(myPublicKey);
    switch (myKeyType) {
      case ECDSA_P_256:
      case ECDSA_P_384:
        m.append(myKeyType.toInteger());

        PKCS10CertificationRequestBuilder csrBuilder =
            new JcaPKCS10CertificationRequestBuilder(new X500NameBuilder().build(), myPublicKey);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
            .build(Objects.requireNonNull(devicePrivateKey(), "privateKey must be non-null"));
        PKCS10CertificationRequest pkcs10 = csrBuilder.build(signer);
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
          pw.writeObject(pkcs10);
        }
        csr = sw.toString();
        break;

      default:
        m.append(KeyType.RSA2048RESTR.toInteger());
    }
    m.append('\00').append(serial);
    m.append('\00').append(model);

    if (null != csr) {
      m.append('\00').append(csr);
    }

    return Base64.getEncoder().encodeToString(m.toString().getBytes(StandardCharsets.US_ASCII));
  }

  // Our private key.
  @Bean
  PrivateKey devicePrivateKey() throws IOException {
    URL url = null != deviceKeyUri ? deviceKeyUri.toURL() : null;
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
      }
    }

    return null;
  }

  // Our public key.  Since this is not secret, it can be effectively static.
  @Bean
  PublicKey devicePublicKey() throws CertificateException, IOException {
    URL url = null != deviceCertificateUri ? deviceCertificateUri.toURL() : null;
    if (null != url) {
      try (InputStream instream = url.openStream()) {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate certificate = cf.generateCertificate(instream);
        return certificate.getPublicKey();
      }
    }

    return null;
  }

  // The Spring ExitCodeExceptionMapper, which we use to map SdoProtocolExceptions
  // to matching 'ec' error code values.
  @Bean
  ExitCodeExceptionMapper exitCodeExceptionMapper() {
    return e -> {
      while (null != e) {
        if (e instanceof ProtocolException) {
          ProtocolException pe = (ProtocolException) e;
          return pe.getError().getEc().toInteger();
        }
        e = e.getCause();
      }
      return -1;
    };
  }

  // The Spring ExitCodeGenerator, for healthy exits.
  @Bean
  ExitCodeGenerator exitCodeGenerator() {
    return () -> 0;
  }

  // The HttpClient which SDO components will use for outgoing HTTP.
  @Bean
  HttpClient httpClient() throws Exception {
    return HttpClient.newBuilder().sslContext(sslContext()).build();
  }

  // Loggers are handy for telling folks about stuff and junk.
  @Bean
  Logger logger() {
    return LoggerFactory.getLogger(DeviceApp.class);
  }

  @Bean
  DeviceInitializationClient sdoDeviceInit() throws Exception {
    return new DeviceInitializationClient(
        httpClient(),
        deviceMark(),
        mfrUri,
        devicePublicKey(),
        secureRandom());
  }

  @Bean
  Supplier<DeviceTransferOwnershipClient> sdoDeviceOnboardFactory()
      throws CertificateException, IOException {

    KeyPair deviceKeys = new KeyPair(devicePublicKey(), devicePrivateKey());
    return () -> {
      try {
        return new DeviceTransferOwnershipClient(
            cipherBlockMode,
            deviceCredentialsStorage().load(),
            httpClient(),
            secureRandom(),
            serviceInfoModules(),
            () -> deviceKeys);
      } catch (Exception e) {
        logger().error(e.getMessage(), e);
        return null;
      }
    };
  }

  @Bean
  SecureRandom secureRandom() {
    for (String algo : secureRandomAlgorithms) {

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

  @Bean
  Collection<?> serviceInfoModules() {
    SdoSysModuleDevice sdosys = new SdoSysModuleDevice();
    sdosys.setExecOutputRedirect(Redirect.INHERIT);

    SdoDevModuleDevice sdodev = new SdoDevModuleDevice();

    return List.of(sdosys, sdodev);
  }

  private void logProperty(String name, String value) {
    if (!(null == value || value.isBlank() || "null".contentEquals(value))) {
      logger().info("property " + name + " = " + value);
    } else {
      logger().info("property " + name + " is not set");
    }
  }

  @Value("${" + PROPERTY_CIPHER_BLOCK_MODE + ":CTR}")
  void setCipherBlockMode(CipherBlockMode cipherBlockMode) {
    if (null != cipherBlockMode) {
      this.cipherBlockMode = cipherBlockMode;
    }

    logProperty(PROPERTY_CIPHER_BLOCK_MODE, Objects.toString(this.cipherBlockMode));
  }

  @Value("${" + PROPERTY_DI_URI + ":}")
  void setMfrUri(String s) {
    if (null != s && !s.isBlank()) {
      this.mfrUri = URI.create(s);
    }

    logProperty(PROPERTY_DI_URI, Objects.toString(this.mfrUri));
  }

  @Value("${" + PROPERTY_DEVICE_MODEL + ":}")
  void setModel(String model) {
    if (null != model && !model.isBlank()) {
      this.model = model;
    }

    logProperty(PROPERTY_DEVICE_MODEL, Objects.toString(this.model));
  }

  @Value("${" + PROPERTY_DEVICE_OUTPUT_DIR + ":}")
  void setOutputDir(String outputDir) {
    if (null != outputDir && !outputDir.isBlank()) {
      this.outputDir = Paths.get(outputDir);
    }

    logProperty(PROPERTY_DEVICE_OUTPUT_DIR, Objects.toString(this.outputDir));
  }

  @Value("${" + PROPERTY_SECURE_RANDOM_ALGORITHMS + ":}")
  void setSecureRandomAlgorithms(@Nullable List<String> secureRandomAlgorithms) {
    if (null != secureRandomAlgorithms && !secureRandomAlgorithms.isEmpty()) {
      this.secureRandomAlgorithms = secureRandomAlgorithms;
    }

    logProperty(PROPERTY_DEVICE_OUTPUT_DIR, Objects.toString(this.secureRandomAlgorithms));
  }

  @Value("${" + PROPERTY_DEVICE_SERIAL_NUMBER + ":}")
  void setSerial(String serial) {
    if (null != serial && !serial.isBlank()) {
      this.serial = serial;
    }

    logProperty(PROPERTY_DEVICE_SERIAL_NUMBER, Objects.toString(this.serial));
  }

  @Value("${" + PROPERTY_STOP_AFTER_DI + ":true}")
  void setStopAfterDi(boolean stopAfterDi) {
    isStopAfterDi = stopAfterDi;

    logProperty(PROPERTY_STOP_AFTER_DI, Objects.toString(this.isStopAfterDi));
  }

  @Value("${" + PROPERTY_DEVICE_CERT + ":}")
  void setdeviceCertificateUri(String s) {
    if (!(null == s || s.isBlank())) {
      URI uri = URI.create(s);
      this.deviceCertificateUri = toAbsolute(uri);
    }

    logProperty(PROPERTY_DEVICE_CERT, Objects.toString(this.deviceCertificateUri));
  }

  @Value("${" + PROPERTY_DEVICE_CREDENTIALS + ":}")
  void setdeviceCredentialsUri(String s) {
    if (!(null == s || s.isBlank())) {
      URI uri = URI.create(s);
      this.deviceCredentialsUri = toAbsolute(uri);
    }

    logProperty(PROPERTY_DEVICE_CREDENTIALS, Objects.toString(this.deviceCredentialsUri));
  }

  @Value("${" + PROPERTY_DEVICE_KEY + ":}")
  void setdeviceKeyUri(String s) {
    if (!(null == s || s.isBlank())) {
      URI uri = URI.create(s);
      this.deviceKeyUri = toAbsolute(uri);
    }

    logProperty(PROPERTY_DEVICE_KEY, Objects.toString(this.deviceKeyUri));
  }

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
    logger().warn("UNSAFE: no-op TrustManager installed");

    return context;
  }
}
