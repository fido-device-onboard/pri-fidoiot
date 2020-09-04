// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.web;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.DispatchResult;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.RendezvousBlobDecoder;
import org.fido.iot.protocol.ServiceInfoEncoder;
import org.fido.iot.protocol.To2ClientService;
import org.fido.iot.protocol.To2ClientStorage;

public class To2ClientApp {

  private static final int RV_PORT = 8040;
  private static final String HOST_NAME = "localhost";
  private static final String PROTOCOL_NAME = "http://";

  static final String devKeyPem = "-----BEGIN CERTIFICATE-----\n"
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
      + "87f51864582054686973206973206120534841323536206b657920666f7220686d616320616c6a44656d6f4"
      + "4657669636550f0956089c0df4c349c61f460457e87eb81858205696c6f63616c686f73748203191f68820c"
      + "018202447f0000018204191f68820858205603b28472872ecbb5d4981fbaa91664ec8627ea395d2bbee7a85"
      + "e0f99a7ed34";

  private static final String rendezvousBlob = ""

      + "84a1012640583a828184447f000001696c6f63616c686f7374191f6a0382085820ce3c1c4cb5660e7a77c932"
      + "3cf332aa44d8fc3eb25bc8e35d7e6a00c801a904ae58483046022100e8567558ac85c022179115803bbc60e9"
      + "bef173d1a7ff526b543e375af5a7e0b1022100d2819d635f658e4e7b1954d2b631a4b25119e8b80c4a3c1935"
      + "aa7d05477b32f3";

  private CryptoService cryptoService;
  private Composite to1dBlob = Composite.fromObject(rendezvousBlob);

  private void run(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {

    cryptoService = new CryptoService();

    MessageDispatcher dispatcher = getDispatcher();

    DispatchResult dr = clientService.getHelloMessage();

    List<String> paths = RendezvousBlobDecoder.getHttpDirectives(to1dBlob);

    for (String path : paths) {

      try {
        WebClient client = new WebClient(path, dr, dispatcher);
        client.run();

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

  private To2ClientStorage clientStorage = new To2ClientStorage() {
    private String clientToken;
    private Composite toOwnerInfo;

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
    public PrivateKey getSigningKey() {
      return PemLoader.loadPrivateKey(devKeyPem);
    }

    @Override
    public Composite getSigInfoA() {
      return cryptoService
          .getSignInfo(
              PemLoader.loadCerts(devKeyPem)
                  .get(0)
                  .getPublicKey());
    }

    @Override
    public byte[] getMaroePrefix() {
      return null;
    }

    @Override
    public String getKexSuiteName() {
      return Const.ECDH_ALG_NAME;
    }

    @Override
    public String getCipherSuiteName() {
      return Const.AES128_CTR_HMAC256_ALG_NAME;
    }

    @Override
    public Composite getReplacementHmac() {
      return null;
    }

    @Override
    public void prepareServiceInfo() {
      Composite value = ServiceInfoEncoder.encodeValue("devmod:active", "true");
      List<Composite> list = new ArrayList<>();
      list.add(value);
      toOwnerInfo = ServiceInfoEncoder.encodeDeviceServiceInfo(list, false);
    }

    @Override
    public Composite getNextServiceInfo() {
      Composite result = toOwnerInfo;
      toOwnerInfo = ServiceInfoEncoder.encodeDeviceServiceInfo(
          Collections.EMPTY_LIST, false);
      return result;
    }

    @Override
    public void setServiceInfo(Composite info, boolean isMore, boolean isDone) {

    }
  };

  To2ClientService clientService = new To2ClientService() {
    @Override
    protected To2ClientStorage getStorage() {
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

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {
    new To2ClientApp().run(args);
    System.out.println("TO2 Client finished.");
    return;
  }
}
