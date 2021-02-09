// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.DispatchResult;
import org.fido.iot.protocol.KeyResolver;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.RendezvousBlobDecoder;
import org.fido.iot.protocol.RendezvousInfoDecoder;
import org.fido.iot.protocol.To0ClientService;
import org.fido.iot.protocol.To0ClientStorage;
import org.fido.iot.storage.OwnerDbTo0Storage;
import org.fido.iot.storage.OwnerDbTo0Util;

public class OwnerTo0Client {

  private final CryptoService cryptoService;
  private final DataSource dataSource;
  private final KeyResolver keyResolver;
  private final OwnerDbTo0Util to0Util;
  private To0ClientStorage clientStorage;
  private To0ClientService clientService;
  private UUID guid;
  private String rvBlob;

  /**
   * Constructor.
   */
  public OwnerTo0Client(CryptoService cryptoService, DataSource dataSource, KeyResolver keyResolver,
      UUID guid, OwnerDbTo0Util to0Util) {
    this.cryptoService = cryptoService;
    this.dataSource = dataSource;
    this.keyResolver = keyResolver;
    this.guid = guid;
    this.to0Util = to0Util;
  }

  public void setRvBlob(String rvBlob) {
    this.rvBlob = rvBlob;
  }

  private To0ClientStorage clientStorage() {
    if (clientStorage == null) {
      clientStorage = new OwnerDbTo0Storage(dataSource, keyResolver, guid) {

        @Override
        public Composite getRedirectBlob() {
          return RendezvousBlobDecoder.decode(rvBlob);
        }
      };
    }
    return clientStorage;
  }

  private To0ClientService clientService() {
    if (clientService == null) {
      clientService = new To0ClientService() {

        @Override
        protected To0ClientStorage getStorage() {
          return clientStorage();
        }

        @Override
        public CryptoService getCryptoService() {
          return cryptoService;
        }
      };
    }
    return clientService;
  }

  private MessageDispatcher createDispatcher() {

    return new MessageDispatcher() {

      @Override
      protected MessagingService getMessagingService(Composite request) {
        return clientService();
      }

      @Override
      protected void failed(Exception e) {
        System.out.println(e.getMessage());
      }
    };
  }

  /**
   * Initiates TO0 for a device.
   */
  public void run() throws NoSuchAlgorithmException, IOException, InterruptedException {
    System.out.println("TO0 Client started for GUID " + guid.toString());
    MessageDispatcher dispatcher = createDispatcher();

    DispatchResult dr = clientService().getHelloMessage();

    Composite ovh = clientStorage().getVoucher().getAsComposite(Const.OV_HEADER);
    Composite rvi = ovh.getAsComposite(Const.OVH_RENDEZVOUS_INFO);

    List<String> paths = RendezvousInfoDecoder.getHttpDirectives(rvi, Const.RV_OWNER_ONLY);

    for (String path : paths) {

      try {
        WebClient client = new WebClient(path, dr, dispatcher);
        client.run();
        long responseWait = this.to0Util.getResponseWait(dataSource, guid);
        if (responseWait > 0) {
          break;
        }
      } catch (Exception e) {
        System.out.println("TO0 failed for " + guid.toString() + "." + e.getMessage());
        throw e;
      }
    }
  }
}
