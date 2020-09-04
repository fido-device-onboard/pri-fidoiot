// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.jupiter.api.Test;
import org.fido.iot.certutils.PemLoader;

public class To0Test extends BaseTemplate {

  private byte[] storedBlob;
  private Composite storedVoucher;
  private Long waitResponse;
  private byte[] storedNonce3;
  private String serverToken = guid.toString();
  private String clientToken;

  @Override
  protected void setup() throws Exception {

    super.setup();
    storedBlob = null;
    storedVoucher = voucher;
    waitResponse = null;
    clientToken = null;

    final To0ClientStorage clientStorage = new To0ClientStorage() {
      @Override
      public Composite getVoucher() {
        return voucher;
      }

      @Override
      public Composite getRedirectBlob() {
        return unsignedRedirect;
      }

      @Override
      public long getRequestWait() {
        return 0;
      }

      @Override
      public void setResponseWait(long wait) {

        waitResponse = wait;
      }

      @Override
      public PrivateKey getOwnerSigningKey(PublicKey ownerPublicKey) {
        return PemLoader.loadPrivateKey(ownerKeyPem);
      }

      @Override
      public void starting(Composite request, Composite reply) {

      }

      @Override
      public void started(Composite request, Composite reply) {
        reply.set(Const.SM_PROTOCOL_INFO,
            Composite.newMap().set(Const.PI_TOKEN, serverToken));
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

    clientService = new To0ClientService() {
      @Override
      protected To0ClientStorage getStorage() {
        return clientStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }
    };

    To0ServerStorage serverStorage = new To0ServerStorage() {
      @Override
      public byte[] getNonce3() {
        return storedNonce3;
      }

      @Override
      public void setNonce3(byte[] nonce3) {

        storedNonce3 = nonce3;
      }

      @Override
      public long storeRedirectBlob(Composite voucher, long requestedWait, byte[] signedBlob) {

        storedBlob = signedBlob;
        return 3600;
      }

      @Override
      public void starting(Composite request, Composite reply) {

      }

      @Override
      public void started(Composite request, Composite reply) {
        reply.set(Const.SM_PROTOCOL_INFO,
            Composite.newMap().set(Const.PI_TOKEN, serverToken));
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

    serverService = new To0ServerService() {
      @Override
      public To0ServerStorage getStorage() {
        return serverStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }
    };
  }

  @Test
  void Test() throws Exception {
    setup();
    runClient(clientService.getHelloMessage());
    assertTrue(waitResponse != null);
    assertTrue(storedBlob != null);

  }

}
