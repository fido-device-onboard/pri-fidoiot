// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.web;

import java.io.Closeable;
import java.io.IOException;
import java.security.Provider;
import java.security.SecureRandom;
import javax.security.auth.message.AuthException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.InvalidJwtException;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.To0ServerService;
import org.fido.iot.protocol.To0ServerStorage;

public class TestListener implements ServletContextListener {

  public static final String BEARER_TOKEN = "1234567890abcef";

  @Override
  public void contextInitialized(ServletContextEvent sce) {

    SecureRandom random = new SecureRandom();
    Provider bc = new BouncyCastleProvider();

    CryptoService cryptoService = new CryptoService();

    To0ServerStorage serverStorage = new To0ServerStorage() {
      byte[] nonce3;

      @Override
      public byte[] getNonce3() {
        return nonce3;
      }

      @Override
      public void setNonce3(byte[] nonce3) {
        this.nonce3 = nonce3;
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
      protected To0ServerStorage getStorage() {
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
        e.printStackTrace();
      }
    }
  }
}
