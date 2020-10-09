// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.DispatchResult;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.RendezvousBlobDecoder;
import org.fido.iot.protocol.RendezvousInfoDecoder;
import org.fido.iot.protocol.To0ClientService;
import org.fido.iot.protocol.To0ClientStorage;

public class To0ClientApp {

  private static final String PROPERTY_OWN_PEM = "fido.iot.pem.own";
  private static final int REQUEST_WS = 3600;
  private static final String RV_BLOB = "http://localhost:8042?ipaddress=127.0.0.1";

  private final To0ClientService myClientService;
  private final To0ClientStorage myClientStorage;
  private final AtomicLong myWaitResponse;

  To0ClientApp(To0ClientService service, To0ClientStorage storage, AtomicLong waitResponse) {
    myClientService = service;
    myClientStorage = storage;
    myWaitResponse = waitResponse;
  }

  /**
   * Application main.
   *
   * @param args
   *     The application arguments.
   */
  public static void main(String[] args) throws Exception {

    Properties properties = System.getProperties();
    String ownerKeyPem = Files.readString(Paths.get(properties.getProperty(PROPERTY_OWN_PEM)));

    AtomicLong waitResponse = new AtomicLong(-1L);

    Composite voucher = Composite.fromObject(System.in.readAllBytes());

    To0ClientStorage clientStorage = new To0ClientStorage() {

      private String clientToken;

      @Override
      public void completed(Composite request, Composite reply) {

      }

      @Override
      public void continued(Composite request, Composite reply) {

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
      public void failed(Composite request, Composite reply) {

      }

      @Override
      public PrivateKey getOwnerSigningKey(PublicKey ownerPublicKey) {
        return PemLoader.loadPrivateKey(ownerKeyPem);
      }

      @Override
      public Composite getRedirectBlob() {
        return RendezvousBlobDecoder.decode(RV_BLOB);
      }

      @Override
      public long getRequestWait() {
        return REQUEST_WS;
      }

      @Override
      public Composite getVoucher() {
        return voucher;
      }

      @Override
      public void setResponseWait(long wait) {
        System.out.println("To0 Response Wait: " + Long.toString(wait));
        waitResponse.set(wait);
      }

      @Override
      public void started(Composite request, Composite reply) {

      }

      @Override
      public void starting(Composite request, Composite reply) {

      }
    };

    CryptoService cryptoService = new CryptoService();
    To0ClientService clientService = new To0ClientService() {

      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }

      @Override
      protected To0ClientStorage getStorage() {
        return clientStorage;
      }
    };
    new To0ClientApp(clientService, clientStorage, waitResponse).run();

    System.out.println("TO0 Client finished.");
    return;
  }

  private MessageDispatcher createDispatcher() {

    return new MessageDispatcher() {

      @Override
      protected void failed(Exception e) {
        e.printStackTrace();
      }

      @Override
      protected MessagingService getMessagingService(Composite request) {
        return myClientService;
      }
    };
  }

  private void run()
      throws NoSuchAlgorithmException, IOException, InterruptedException {

    MessageDispatcher dispatcher = createDispatcher();

    DispatchResult dr = myClientService.getHelloMessage();

    Composite ovh = myClientStorage.getVoucher().getAsComposite(Const.OV_HEADER);
    Composite rvi = ovh.getAsComposite(Const.OVH_RENDEZVOUS_INFO);

    List<String> paths = RendezvousInfoDecoder.getHttpDirectives(
        rvi,
        Const.RV_OWNER_ONLY);

    for (String path : paths) {
      try {
        WebClient client = new WebClient(path, dr, dispatcher);
        client.run();
        if (myWaitResponse.get() >= 0L) {
          break;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }
}
