// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.net.ConnectException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.DispatchResult;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.RendezvousBlobDecoder;
import org.fidoalliance.fdo.protocol.RendezvousInfoDecoder;
import org.fidoalliance.fdo.protocol.To0ClientService;
import org.fidoalliance.fdo.protocol.To0ClientStorage;
import org.fidoalliance.fdo.storage.OwnerDbTo0Storage;
import org.fidoalliance.fdo.storage.OwnerDbTo0Util;

public class OwnerTo0Client {

  private final CryptoService cryptoService;
  private final DataSource dataSource;
  private final KeyResolver keyResolver;
  private final OwnerDbTo0Util to0Util;
  private To0ClientStorage clientStorage;
  private To0ClientService clientService;
  private UUID guid;
  private String rvBlob;
  private static LoggerService logger;

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
    this.logger = new LoggerService(OwnerTo0Client.class);
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
    };
  }

  /**
   * Initiates TO0 for a device.
   */
  public void run() throws Exception {
    logger.info("TO0 Client started for GUID " + guid.toString());
    MessageDispatcher dispatcher = createDispatcher();

    DispatchResult dr = clientService().getHelloMessage();

    Composite ovh = clientStorage().getVoucher().getAsComposite(Const.OV_HEADER);
    Composite rvi = ovh.getAsComposite(Const.OVH_RENDEZVOUS_INFO);

    List<String> paths = RendezvousInfoDecoder.getHttpDirectives(rvi, Const.RV_OWNER_ONLY);

    if (paths.size() == 0) {
      logger.error("No Directives found. Invalid RVInfo Blob in " + guid.toString());
      throw new IOException("TO0 failed for " + guid.toString() + ".");
    }

    // iterate through all paths and throw an exception only if there are no more paths
    // left to try
    String path = null;
    Iterator<String> pathIterator = paths.listIterator();
    while (pathIterator.hasNext()) {

      path = pathIterator.next();
      try {
        WebClient client = new WebClient(path, dr, dispatcher);
        client.call();
        long responseWait = this.to0Util.getResponseWait(dataSource, guid);
        if (responseWait > 0) {
          break;
        }
      } catch (ConnectException e) {
        logger.error("Unable to connect with RV at " + path + ". " + e.getMessage());
        if (!pathIterator.hasNext()) {
          throw e;
        }
      } catch (Exception e) {
        logger.error("TO0 failed for " + guid.toString() + "." + e.getMessage());
        if (!pathIterator.hasNext()) {
          throw e;
        }
      }
    }
  }
}
