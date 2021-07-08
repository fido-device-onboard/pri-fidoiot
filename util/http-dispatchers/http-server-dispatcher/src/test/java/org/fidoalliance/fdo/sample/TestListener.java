// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.Closeable;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import javax.security.auth.message.AuthException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.InvalidJwtException;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.To0ServerService;
import org.fidoalliance.fdo.protocol.To0ServerStorage;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;

public class TestListener implements ServletContextListener {

  public static final String BEARER_TOKEN = "1234567890abcef";
  private static final LoggerService logger = new LoggerService(TestListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {

    try {
      SecureRandom random = SecureRandom.getInstanceStrong();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    Provider bc = new BouncyCastleProvider();

    CryptoService cryptoService = new CryptoService();

    To0ServerStorage serverStorage = new To0ServerStorage() {
      byte[] nonceTo0Sign;

      @Override
      public byte[] getNonceTo0Sign() {
        return nonceTo0Sign;
      }

      @Override
      public void setNonceTo0Sign(byte[] nonceTo0Sign) {
        this.nonceTo0Sign = nonceTo0Sign;
      }

      @Override
      public long storeRedirectBlob(Composite voucher, long requestedWait, byte[] signedBlob) {
        return 60;
      }

      @Override
      public void starting(Composite request, Composite reply) {

      }

      @Override
      public void started(Composite request, Composite reply) {

        reply.set(Const.SM_PROTOCOL_INFO,
            Composite.newMap().set(Const.PI_TOKEN, TestListener.BEARER_TOKEN));
      }

      @Override
      public void continuing(Composite request, Composite reply) {

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

    To0ServerService to0Service = new To0ServerService() {
      @Override
      public To0ServerStorage getStorage() {
        return serverStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }
    };

    MessageDispatcher dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        Composite info = request.getAsComposite(Const.SM_PROTOCOL_INFO);
        if (info.containsKey(Const.PI_TOKEN)) {
          String token = info.getAsString(Const.PI_TOKEN);
          if (!token.equals(BEARER_TOKEN)) {
            throw new InvalidJwtException(new AuthException());
          }
        }
        return to0Service;
      }

      ;
    };
    //create a message dispatcher dispatcher
    String name = sce.getServletContext().getServletContextName();
    sce.getServletContext().setAttribute(Const.DISPATCHER_ATTRIBUTE, dispatcher);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

    Object obj = sce.getServletContext().getAttribute(Const.DISPATCHER_ATTRIBUTE);
    if (obj != null && obj instanceof Closeable) {
      try {
        ((Closeable) obj).close();
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }
  }
}
