// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.List;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.RendezvousInfoDecoder;
import org.fidoalliance.fdo.protocol.To1ClientService;
import org.fidoalliance.fdo.protocol.To1ClientStorage;

public class To1ClientApp {

  private static final LoggerService logger = new LoggerService(To1ClientApp.class);

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
      + "87f51864582054686973206973206120534841323536206b657920666f7220686d616320616c6a44656d6f446"
      + "576696365506d955aaa8fb142c18e1123465357a41c81858205696c6f63616c686f73748203191f68820c0182"
      + "02447f00000182041920fb822f58209172fd0feaa577bb039d321d9011fe4f10959becbf5adab1d4cbcbb6fce"
      + "67877";

  final CryptoService cryptoService = new CryptoService();
  Composite signedTo1Blob;

  private void run(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {

    try {
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
          client.call();
          if (signedTo1Blob != null) {
            break;
          }
        } catch (Exception e) {
          logger.error("Unable to contact RV at " + path);
        }
      }
    } catch (Exception e) {
      logger.error("T01 failed. Exiting application.");
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
      logger.info("signed RV Blob: " + signedBlob.toString());
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
        logger.error(e.getMessage());
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
    logger.info("TO1 Client finished.");
    return;
  }
}
