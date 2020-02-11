// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri.to0client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CRL;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PKIXRevocationChecker.Option;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import org.sdo.cri.OwnerLocationInfo;
import org.sdo.cri.OwnerTransferOwnershipClient;
import org.sdo.cri.OwnershipVoucher;
import org.sdo.cri.OwnershipVoucherParser;
import org.sdo.cri.ProtocolException;
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
import org.springframework.lang.Nullable;

@SpringBootApplication
public class To0ClientApp {

  private InetAddress advertisedOwnerAddress = getLocalAddress();
  private String advertisedOwnerHostname = getLocalAddress().getHostName();
  private int advertisedOwnerPort = 0;
  private Set<URI> myCrlUris = null;
  private boolean isRevocationCheckingEnabled = false;
  private URI ownerCertificateUri = null;
  private URI ownerKeyUri = null;
  // Set default revocation options to the common case: check CRLs, then a soft (undecided) fail.
  private Set<PKIXRevocationChecker.Option> revocationCheckerOptions =
      EnumSet.of(Option.PREFER_CRLS, Option.SOFT_FAIL, Option.NO_FALLBACK);
  private List<String> secureRandomAlgorithms = List.of("NativePRNG", "Windows-PRNG", "SHA1PRNG");
  private Set<URI> myTrustAnchorUris = Set.of();
  private Duration waitSeconds = Duration.ofHours(1);

  /**
   * The application entry point.
   *
   * @param args The command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(To0ClientApp.class);
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
  ApplicationRunner applicationRunner() throws IOException, CertificateException {
    KeyPair keys = new KeyPair(ownerPublicKey(), ownerPrivateKey());
    return args -> {
      // args is one or more ownership voucher (.op) files to run TO0 on
      for (String arg : args.getNonOptionArgs()) {
        final OwnershipVoucher voucher;

        try (FileReader reader = new FileReader(new File(arg))) {
          voucher = new OwnershipVoucherParser().readObject(reader);
        } catch (IOException e) {
          logger().error(e.getMessage());
          continue;
        }

        OwnerTransferOwnershipClient ownerClient = new OwnerTransferOwnershipClient(
            httpClient(),
            voucher,
            certPathValidator(),
            ownerLocationInfo(),
            type -> keys,
            waitSecondsBuilder()
        );
        ownerClient.call();
      } // foreach arg
    };
  }

  @Bean
  Predicate<CertPath> certPathValidator() throws Exception {
    if (isRevocationCheckingEnabled) {

      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

      Set<CRL> crls = new HashSet<>();

      for (URI uri : myCrlUris) {
        Collection<File> files = findRegularFiles(uri);
        for (File file : files) {
          try (FileInputStream inputStream = new FileInputStream(file)) {
            crls.addAll(certificateFactory.generateCRLs(inputStream));
          }
        }
      } // foreach uri

      CollectionCertStoreParameters params = new CollectionCertStoreParameters(crls);
      final CertStore certStore = CertStore.getInstance("Collection", params);

      Set<TrustAnchor> trustAnchors = new HashSet<>();

      for (URI uri : myTrustAnchorUris) {
        Collection<File> files = findRegularFiles(uri);
        for (File file : files) {
          try (FileInputStream inputStream = new FileInputStream(file)) {
            certificateFactory.generateCertificates(inputStream).stream()
                .filter(cert -> cert instanceof X509Certificate)
                .map(X509Certificate.class::cast)
                .map(cert -> new TrustAnchor(cert, null))
                .forEach(trustAnchors::add);
          }
        }
      } // foreach uri

      // BouncyCastle does not provide CertPathValidatorSpi::engineGetRevocationChecker
      // so we must explicitly use the SUN provider
      CertPathValidator cpv = CertPathValidator.getInstance("PKIX", "SUN");
      PKIXRevocationChecker rc = (PKIXRevocationChecker) cpv.getRevocationChecker();
      rc.setOptions(revocationCheckerOptions);
      PKIXParameters certPathParameters = new PKIXParameters(trustAnchors);
      certPathParameters.addCertPathChecker(rc);
      certPathParameters.addCertStore(certStore);
      certPathParameters.setRevocationEnabled(true);

      return certPath -> {
        try {
          cpv.validate(certPath, certPathParameters);
          return true; // If it didn't throw an exception, test passes
        } catch (CertPathValidatorException | InvalidAlgorithmParameterException e) {
          logger().error(e.getMessage(), e);
          return false;
        } finally {
          List<CertPathValidatorException> softErrs = rc.getSoftFailExceptions();
          logger().debug(softErrs.toString());
        }
      };

    } else {
      return certPath -> true;
    }
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
    return LoggerFactory.getLogger(getClass());
  }

  @Bean
  OwnerLocationInfo ownerLocationInfo() {
    return new OwnerLocationInfo(
        advertisedOwnerAddress,
        advertisedOwnerHostname,
        advertisedOwnerPort);
  }

  // Our private key.
  @Bean
  PrivateKey ownerPrivateKey() throws IOException {
    URL url = null != ownerKeyUri ? ownerKeyUri.toURL() : null;
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
  PublicKey ownerPublicKey() throws CertificateException, IOException {
    URL url = null != ownerCertificateUri ? ownerCertificateUri.toURL() : null;
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    if (null != url) {
      var cert = cf.generateCertificate(url.openStream());
      return cert.getPublicKey();
    }

    return null;
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

  @Value("${org.sdo.to0.ownersign.to1d.bo.i1:}")
  void setAdvertisedOwnerAddress(String s) throws UnknownHostException {
    if (!(null == s || s.isBlank())) {
      this.advertisedOwnerAddress = InetAddress.getByName(s);
    }
  }

  @Value("${org.sdo.to0.ownersign.to1d.bo.dns1:}")
  void setAdvertisedOwnerHostname(String s) {
    if (null != s) {
      this.advertisedOwnerHostname = s;
    }
  }

  @Value("${org.sdo.to0.ownersign.to1d.bo.port1:0}")
  void setAdvertisedOwnerPort(String s) {
    if (!(null == s || s.isBlank())) {
      this.advertisedOwnerPort = Integer.parseUnsignedInt(s);
    }
  }

  @Value("${org.sdo.pkix.crls:}")
  void setCrlUris(List<String> s) {
    if (null != s) {
      this.myCrlUris = s.stream().map(URI::create).collect(Collectors.toSet());
    }
  }

  @Value("${org.sdo.owner.cert:}")
  void setOwnerCertificateUri(URI ownerCertificateUri) {
    if (null != ownerCertificateUri) {
      this.ownerCertificateUri = toAbsolute(ownerCertificateUri);
    }
  }

  @Value("${org.sdo.owner.key:}")
  void setOwnerKeyUri(URI ownerKeyUri) {
    if (null != ownerKeyUri) {
      this.ownerKeyUri = toAbsolute(ownerKeyUri);
    }
  }

  @Value("${org.sdo.pkix.revocation-options:}")
  void setRevocationCheckerOptions(List<String> s) {
    if (null != s) {
      this.revocationCheckerOptions =
          s.stream().map(PKIXRevocationChecker.Option::valueOf).collect(Collectors.toSet());
    }
  }

  @Value("${org.sdo.pkix.revocation-checking-enabled:}")
  void setRevocationCheckingEnabled(String s) {
    if (!(null == s || s.isBlank())) {
      this.isRevocationCheckingEnabled = Boolean.parseBoolean(s);
    }
  }

  @Value("${org.sdo.secure-random:}")
  void setSecureRandomAlgorithms(@Nullable List<String> secureRandomAlgorithms) {
    if (null != secureRandomAlgorithms && !secureRandomAlgorithms.isEmpty()) {
      this.secureRandomAlgorithms = secureRandomAlgorithms;
    }
  }

  @Value("${org.sdo.pkix.trust-anchors:}")
  void setTrustAnchorUris(List<String> s) {
    if (null != s) {
      this.myTrustAnchorUris = s.stream().map(URI::create).collect(Collectors.toSet());
    }
  }

  @Value("${org.sdo.to0.ownersign.to0d.ws:}")
  void setWaitSeconds(String waitSecondsText) {
    if (null != waitSecondsText && !waitSecondsText.isBlank()) {
      this.waitSeconds = Duration.parse(waitSecondsText);
    }
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
    LoggerFactory.getLogger(getClass()).warn("UNSAFE: no-op TrustManager installed");

    return context;
  }

  // The WaitSecondsBuilder, which produces a wait-seconds value given an ownership voucher.
  // This allows a custom delay to be requested for each device.
  // This simple implementation always asks for the same delay.
  @Bean
  Function<OwnershipVoucher, Duration> waitSecondsBuilder() {
    return (voucher) -> waitSeconds;
  }

  private Collection<File> findRegularFiles(URI uri) throws IOException {
    Path path = Paths.get(uri);
    if (Files.isDirectory(path)) { // if it's a directory, check all the files in it
      return Files.list(path)
          .filter(Files::isRegularFile)
          .map(Path::toFile)
          .collect(Collectors.toList());
    } else if (Files.isRegularFile(path)) { // if it's a file...
      return List.of(path.toFile());
    } else {
      return Collections.emptyList();
    }
  }

  private InetAddress getLocalAddress() {
    // This odd-looking socket doesn't actually connect to the outside world,
    // nor does the target IP need to be reachable.  By putting a datagram socket
    // into a connect state, we can determine our outgoing network interface.
    try (final DatagramSocket socket = new DatagramSocket()) {
      socket.connect(InetAddress.getByName("8.8.8.8"), 8888);
      return socket.getLocalAddress();
    } catch (Exception e) {
      return InetAddress.getLoopbackAddress();
    }
  }
}
