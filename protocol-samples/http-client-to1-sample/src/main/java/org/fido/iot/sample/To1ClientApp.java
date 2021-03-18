// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.List;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.RendezvousInfoDecoder;
import org.fido.iot.protocol.To1ClientService;
import org.fido.iot.protocol.To1ClientStorage;

public class To1ClientApp {

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

  private static final String deviceCreds = ""
      + "87f5186458401124dc2801de1a8d31978c0efc205871469cbe851eb226838219ffa66f0c1ffc3550bdfdb50fe"
      + "b58b8573b062769b85a08ae9cb18bcf1e5d5721f0e73c8ba9a36b4a61766120446576696365502e1418ebc810"
      + "49c4a0db91c4d0a5830681858205696c6f63616c686f73748203191f68820c018202447f0000018204191f688"
      + "20858205603b28472872ecbb5d4981fbaa91664ec8627ea395d2bbee7a85e0f99a7ed34";

  final CryptoService cryptoService = new CryptoService();
  Composite signedTo1Blob;

  private void run(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {

    final MessageDispatcher dispatcher = getDispatcher();

    List<String> paths = RendezvousInfoDecoder.getHttpDirectives(
        clientStorage.getDeviceCredentials().getAsComposite(Const.DC_RENDEZVOUS_INFO),
        Const.RV_DEV_ONLY);

    // Setting epid test mode enables epid signatures from debug and test
    // devices to pass validation. In production, this should never be used.
    cryptoService.setEpidTestMode();

    signedTo1Blob = null;
    for (String path : paths) {

      try {
        WebClient client = new WebClient(path, clientService.getHelloMessage(), dispatcher);
        client.run();
        if (signedTo1Blob != null) {
          break;
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }

  }

  private To1ClientStorage clientStorage = new To1ClientStorage() {
    private String clientToken;

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

    @Override
    public Composite getDeviceCredentials() {
      return Composite.fromObject(deviceCreds);
    }

    @Override
    public Composite getSigInfoA() {
      return cryptoService.getSignInfo(
          PemLoader.loadCerts(devKeyPem)
              .get(0)
              .getPublicKey());
    }

    @Override
    public PrivateKey getSigningKey() {
      return PemLoader.loadPrivateKey(devKeyPem);
    }

    @Override
    public byte[] getMaroePrefix() {
      return null;
    }

    @Override
    public void storeSignedBlob(Composite signedBlob) {
      signedTo1Blob = signedBlob;
      System.out.println("signed RV Blob: " + signedBlob.toString());
    }
  };

  private To1ClientService clientService = new To1ClientService() {

    @Override
    protected To1ClientStorage getStorage() {
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
        System.out.println(e.getMessage());
      }
    };
  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {
    new To1ClientApp().run(args);
    System.out.println("TO1 Client finished.");
    return;
  }
}
