package org.fido.iot.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.DiClientService;
import org.fido.iot.protocol.DiClientStorage;
import org.fido.iot.protocol.DispatchResult;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.RendezvousBlobDecoder;
import org.fido.iot.protocol.RendezvousInfoDecoder;
import org.fido.iot.protocol.ServiceInfoEncoder;
import org.fido.iot.protocol.To1ClientService;
import org.fido.iot.protocol.To1ClientStorage;
import org.fido.iot.protocol.To2ClientService;
import org.fido.iot.protocol.To2ClientStorage;

public class Device {

  private static final String PROPERTY_CREDENTIAL = "fido.iot.device.credential";
  private static final String PROPERTY_DEV_PEM = "fido.iot.pem.dev";
  private static final String PROPERTY_DI_URL = "fido.iot.url.di";
  private static final String PROPERTY_RANDOMS = "fido.iot.randoms";

  private static final Logger logger = LogManager.getLogger();

  final CryptoService myCryptoService;
  final KeyPair myKeys;
  final CredentialStorage myCredStore;

  Device() throws IOException {
    Properties p = System.getProperties();

    myCredStore =
        new CredentialStorage(Paths.get(p.getProperty(PROPERTY_CREDENTIAL, "credential.bin")));

    String randoms = p.getProperty(PROPERTY_RANDOMS, "NativePRNG,Windows-PRNG");
    myCryptoService = new CryptoService(randoms.split(","));

    String pem = Files.readString(
        Paths.get(p.getProperty(PROPERTY_DEV_PEM, "device.pem")));
    myKeys = new KeyPair(PemLoader.loadPublicKeys(pem).get(0), PemLoader.loadPrivateKey(pem));
  }

  /**
   * The shell entry-point for the Java Device.
   *
   * @param argv
   *     The shell ARGV list.
   * @throws Exception
   *     If the protocol fails for any reason.
   */
  public static void main(String[] argv) throws Exception {
    new Device().run();
  }

  protected byte[] buildCsr() {

    PKCS10CertificationRequestBuilder csrBuilder;
    csrBuilder = new JcaPKCS10CertificationRequestBuilder(
        new X500NameBuilder().build(), myKeys.getPublic());

    try {
      String signatureAlg = myCryptoService.getSignatureAlgorithm(
          myCryptoService.getCoseAlgorithm(myKeys.getPublic()));
      ContentSigner signer = new JcaContentSignerBuilder(signatureAlg)
          .build(Objects.requireNonNull(myKeys.getPrivate(), "privateKey must be non-null"));
      PKCS10CertificationRequest pkcs10 = csrBuilder.build(signer);
      return pkcs10.getEncoded();

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected String getModel() {
    return "Java Device";
  }

  protected String getSerial() {
    return "0";
  }

  byte[] createSecret() {
    return myCryptoService.getRandomBytes(384 / 8);
  }

  void doDeviceInit() {

    // Start with 'blank' credentials.  The protocol library will overwrite them as it goes.
    Composite credentials = Composite.newArray()
        .set(Const.DC_ACTIVE, true)
        .set(Const.DC_PROTVER, Const.PROTOCOL_VERSION_100)
        .set(Const.DC_HMAC_SECRET, createSecret())
        .set(Const.DC_DEVICE_INFO, "")
        .set(Const.DC_GUID, new UUID(0, 0))
        .set(Const.DC_RENDEZVOUS_INFO, Composite.newMap())
        .set(Const.DC_PUBLIC_KEY_HASH, Composite.newArray());

    DiClientStorage storage = new DiClientStorage() {
      @Override
      public void completed(Composite request, Composite reply) {
        logger.info("DI complete, GUID is " + credentials.getAsUuid(Const.DC_GUID));

        // The protocol library expects the credential composite to be write-through.
        // Once the protocol completes, we must make sure to write the credential back to disk
        // or we'll lose the changes that were made.
        //
        try {
          myCredStore.store(credentials);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Composite getDeviceCredentials() {
        return credentials;
      }

      @Override
      public Object getDeviceMfgInfo() {
        return Composite.newArray()
            .set(Const.FIRST_KEY, myCryptoService.getPublicKeyType(myKeys.getPublic()))
            .set(Const.SECOND_KEY, getSerial())
            .set(Const.THIRD_KEY, getModel())
            .set(Const.FOURTH_KEY, buildCsr());
      }

      @Override
      public void started(Composite request, Composite reply) {

      }

      @Override
      public void starting(Composite request, Composite reply) {

      }
    };

    DiClientService service = new DiClientService() {
      @Override
      public CryptoService getCryptoService() {
        return myCryptoService;
      }

      @Override
      protected DiClientStorage getStorage() {
        return storage;
      }
    };

    MessageDispatcher dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return service;
      }
    };

    String url = System.getProperties().getProperty(PROPERTY_DI_URL, "http://localhost:8039/");
    logger.info("DI URL is " + url);

    DispatchResult dr = service.getHelloMessage();
    WebClient client = new WebClient(url, dr, dispatcher);
    client.run();
  }

  void doTransferOwnership(Composite credentials) {

    Composite rvi = credentials.getAsComposite(Const.DC_RENDEZVOUS_INFO);
    List<String> to1Urls = RendezvousInfoDecoder.getHttpDirectives(rvi, Const.RV_DEV_ONLY);

    // the 'signed blob' contains the redirect to TO2
    AtomicReference<Composite> signedBlob = new AtomicReference<>();
    AtomicReference<Composite> wrappedCreds = new AtomicReference<>(credentials);
    AtomicBoolean isTo2Done = new AtomicBoolean(false);

    To1ClientStorage to1Storage = new To1ClientStorage() {

      @Override
      public void completed(Composite request, Composite reply) {
        List<String> redirects = RendezvousBlobDecoder.getHttpDirectives(signedBlob.get());
        logger.info("TO1 complete, owner is at " + redirects);
      }

      @Override
      public Composite getDeviceCredentials() {
        return wrappedCreds.get();
      }

      @Override
      public byte[] getMaroePrefix() {
        return new byte[0];
      }

      @Override
      public Composite getSigInfoA() {
        return Composite.newArray();
      }

      @Override
      public PrivateKey getSigningKey() {
        return myKeys.getPrivate();
      }

      @Override
      public void storeSignedBlob(Composite b) {
        signedBlob.set(b.clone());
      }
    };

    To1ClientService to1Service = new To1ClientService() {
      @Override
      public CryptoService getCryptoService() {
        return myCryptoService;
      }

      @Override
      protected To1ClientStorage getStorage() {
        return to1Storage;
      }
    };

    MessageDispatcher to1Dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return to1Service;
      }
    };

    for (String url : to1Urls) {
      if (null != signedBlob.get()) {
        break;
      }

      logger.info("TO1 URL is " + url);

      DispatchResult dr = to1Service.getHelloMessage();
      WebClient client = new WebClient(url, dr, to1Dispatcher);
      client.run();
    }

    To2ClientStorage to2Storage = new To2ClientStorage() {

      @Override
      public void completed(Composite request, Composite reply) {
        logger.info("TO2 complete, GUID is " + wrappedCreds.get().getAsUuid(Const.DC_GUID));

        try {
          myCredStore.store(wrappedCreds.get());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        isTo2Done.set(true);
      }

      @Override
      public String getCipherSuiteName() {
        return Const.AES128_CTR_HMAC256_ALG_NAME;
      }

      @Override
      public Composite getDeviceCredentials() {
        return credentials;
      }

      @Override
      public String getKexSuiteName() {
        return Const.ECDH_ALG_NAME;
      }

      @Override
      public byte[] getMaroePrefix() {
        return new byte[0];
      }

      @Override
      public Composite getNextServiceInfo() {
        return ServiceInfoEncoder.encodeDeviceServiceInfo(Collections.emptyList(), false);
      }

      @Override
      public byte[] getReplacementHmacSecret(Composite newCredentials, boolean isReuse) {
        if (!isReuse) {
          byte[] newSecret = createSecret();
          wrappedCreds.set(newCredentials.clone().set(Const.DC_HMAC_SECRET, newSecret));
          return newSecret;

        } else {
          logger.info("reuse enabled, credentials will not be changed");
          return null;
        }
      }

      @Override
      public Composite getSigInfoA() {
        return Composite.newArray();
      }

      @Override
      public PrivateKey getSigningKey() {
        return myKeys.getPrivate();
      }

      @Override
      public void prepareServiceInfo() {

      }

      @Override
      public void setServiceInfo(Composite info, boolean isMore, boolean isDone) {

      }
    };

    To2ClientService to2Service = new To2ClientService() {
      @Override
      public CryptoService getCryptoService() {
        return myCryptoService;
      }

      @Override
      protected To2ClientStorage getStorage() {
        return to2Storage;
      }
    };

    MessageDispatcher to2Dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return to2Service;
      }
    };

    List<String> to2Urls = RendezvousBlobDecoder.getHttpDirectives(signedBlob.get());

    for (String url : to2Urls) {
      if (isTo2Done.get()) {
        break;
      }

      logger.info("TO2 URL is " + url);

      DispatchResult dr = to2Service.getHelloMessage();
      WebClient client = new WebClient(url, dr, to2Dispatcher);
      client.run();
    }
  }

  void run() {

    try {
      Composite credentials = myCredStore.load();
      logger.info("credentials loaded, GUID is " + credentials.getAsUuid(Const.DC_GUID));
      if (credentials != null) {
        doTransferOwnership(credentials);
      }
    } catch (IOException e) {
      doDeviceInit();
    }
  }
}
