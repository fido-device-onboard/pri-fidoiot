// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DiTest extends BaseTemplate {

  private Composite storedVoucher;
  private String clientToken;
  private String serverToken = guid.toString();

  protected void setup() throws Exception {
    super.setup();
    storedVoucher = null;

    final DiClientStorage clientStorage = new DiClientStorage() {

      @Override
      public Object getDeviceMfgInfo() {
        return null;
      }

      @Override
      public Composite getDeviceCredentials() {
        return Composite.newArray()
            .set(Const.DC_ACTIVE, true)
            .set(Const.DC_PROTVER, Const.PROTOCOL_VERSION_100)
            .set(Const.DC_HMAC_SECRET, devSecret);
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

    clientService = new DiClientService() {
      @Override
      protected DiClientStorage getStorage() {
        return clientStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }
    };

    DiServerStorage serverStorage = new DiServerStorage() {

      @Override
      public Composite createVoucher(Object mfgInfo) {
        return voucher;
      }

      @Override
      public Composite getVoucher() {
        return voucher;
      }

      @Override
      public void storeVoucher(Composite voucher) {
        storedVoucher = voucher;
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

    serverService = new DiServerService() {
      @Override
      protected DiServerStorage getStorage() {
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
    assertTrue(storedVoucher != null);

  }

}
