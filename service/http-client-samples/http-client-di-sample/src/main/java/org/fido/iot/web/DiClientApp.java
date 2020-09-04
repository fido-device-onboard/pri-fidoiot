// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.CloseableKey;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.DiClientService;
import org.fido.iot.protocol.DiClientStorage;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;

public class DiClientApp {

  private static final String DI_URI = "http://localhost:8039";

  private static final String DEVICE_INFO = "JavaDevice";

  protected static final byte[] devSecret =
      "This is a SHA256 key for hmac al".getBytes(StandardCharsets.US_ASCII);

  private static final String devKeyPem = "-----BEGIN CERTIFICATE-----\n"
      + "MIIBdjCCAR0CCQCNo1W35xxR9TAKBggqhkjOPQQDAjANMQswCQYDVQQDDAJDQTAg\n"
      + "Fw0xOTExMjIxNTU0MDFaGA8yMDU0MTExMzE1NTQwMVoweDELMAkGA1UEBhMCVVMx\n"
      + "DzANBgNVBAgMBk9yZWdvbjESMBAGA1UEBwwJSGlsbHNib3JvMQ4wDAYDVQQKDAVJ\n"
      + "bnRlbDEdMBsGA1UECwwURGV2aWNlIE1hbnVmYWN0dXJpbmcxFTATBgNVBAMMDERl\n"
      + "bW9EZXZpY2UyNzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABKWC8HLsakdG2OfJ\n"
      + "dFWKbE7GlM6RQgqXjd25ldIB6ecSxzMLwRUcjrZWMTdF2sfHBA7H7yLlSWIWMrWz\n"
      + "hj5GfJgwCgYIKoZIzj0EAwIDRwAwRAIgQ4YHfzmu55T35I6vBP9MGIIqjDBplK1K\n"
      + "11zKta73R4wCIHPOGDQpRSZiwp1MTRt1D2MWfoXJyw73slgamG7JKCvx\n"
      + "-----END CERTIFICATE-----\n"
      + "-----BEGIN EC PARAMETERS-----\n"
      + "BggqhkjOPQMBBw==\n"
      + "-----END EC PARAMETERS-----\n"
      + "-----BEGIN EC PRIVATE KEY-----\n"
      + "MHcCAQEEIH5hKR4Yit57JC0SVpIyAUtrHnnHcYEzDHLrs5ogHWgtoAoGCCqGSM49\n"
      + "AwEHoUQDQgAEpYLwcuxqR0bY58l0VYpsTsaUzpFCCpeN3bmV0gHp5xLHMwvBFRyO\n"
      + "tlYxN0Xax8cEDsfvIuVJYhYytbOGPkZ8mA==\n"
      + "-----END EC PRIVATE KEY-----";

  private final CryptoService cryptoService = new CryptoService();

  private final Composite deviceCredentials = Composite.newArray()
      .set(Const.DC_ACTIVE, true)
      .set(Const.DC_PROTVER, Const.PROTOCOL_VERSION_100)
      .set(Const.DC_HMAC_SECRET, devSecret);

  private DiClientStorage clientStorage = new DiClientStorage() {
    private String clientToken;

    @Override
    public Object getDeviceMfgInfo() {
      PKCS10CertificationRequestBuilder csrBuilder =
          new JcaPKCS10CertificationRequestBuilder(new X500NameBuilder().build(),
              PemLoader.loadCerts(devKeyPem).get(0).getPublicKey());

      byte[] csr = null;
      try (CloseableKey key =
          new CloseableKey(
              PemLoader.loadPrivateKey(devKeyPem))) {
        ContentSigner signer = new JcaContentSignerBuilder(Const.ECDSA_256_ALG_NAME)
            .build(key.get());
        PKCS10CertificationRequest pkcs10 = csrBuilder.build(signer);
        csr = pkcs10.getEncoded();

        String serialNo = Composite.toString(
            cryptoService.getRandomBytes(4));

        System.out.println("SerialNo: " + serialNo);

        return Composite.newArray()
            .set(Const.FIRST_KEY, Const.PK_SECP256R1)
            .set(Const.SECOND_KEY, serialNo)
            .set(Const.THIRD_KEY, "DemoDevice")
            .set(Const.FOURTH_KEY, csr);

      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      } catch (OperatorCreationException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    @Override
    public Composite getDeviceCredentials() {
      return deviceCredentials;
    }

    @Override
    public void starting(Composite request, Composite reply) {

    }

    @Override
    public void started(Composite request, Composite reply) {

    }

    @Override
    public void continuing(Composite request, Composite reply) {
      Composite info = request.getAsComposite(Const.SM_PROTOCOL_INFO);
      if (info.containsKey(Const.PI_TOKEN)) {
        clientToken = info.getAsString(Const.PI_TOKEN);
      }
      reply.set(Const.SM_PROTOCOL_INFO,
          Composite.newMap().set(Const.PI_TOKEN, clientToken));
    }

    @Override
    public void continued(Composite request, Composite reply) {

    }

    @Override
    public void completed(Composite request, Composite reply) {

    }

    @Override
    public void failed(Composite request, Composite reply) {

    }
  };

  private DiClientService clientService = new DiClientService() {

    @Override
    protected DiClientStorage getStorage() {
      return clientStorage;
    }

    @Override
    public CryptoService getCryptoService() {
      return cryptoService;
    }
  };

  private MessageDispatcher getDispatcher() {

    return new MessageDispatcher() {

      @Override
      protected MessagingService getMessagingService(Composite request) {
        return clientService;
      }

      @Override
      protected void failed(Exception e) {
        e.printStackTrace();
      }
    };
  }

  private void run(String[] args) {

    final MessageDispatcher dispatcher = getDispatcher();

    try {
      WebClient client = new WebClient(DI_URI, clientService.getHelloMessage(), dispatcher);
      client.run();
      System.out.println("Device Credentials: " + deviceCredentials);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args) {
    new DiClientApp().run(args);
    System.out.println("DI Client finished.");
    return;
  }
}
