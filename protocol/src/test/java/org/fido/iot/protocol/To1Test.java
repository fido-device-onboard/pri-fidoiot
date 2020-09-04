// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.fido.iot.certutils.PemLoader;

public class To1Test extends BaseTemplate {

  private byte[] storedNonce4;
  private Composite storedBlob;
  private UUID storedGuid;
  private String serverToken;
  private String clientToken;
  private String sererToken = guid.toString();

  private final String signedBlobData = ""
      + "84a1012640583a828184447f000001696c6f63616c686f7374191f6803820858205dd275b97a8d3669a714"
      + "f29d9086967587cc9cd33211d3f2d48e0e0edd9667a358473045022100da8b8e4dc03bb2329224c8d90a8f"
      + "c9925ce3fb853c44f4cc39b5f9c2df2b007402200c7b57db39de48dfb04c9d6d95e9e354048bd75e164ab2"
      + "ec909e7af4818122e2";

  @Override
  protected void setup() throws Exception {

    super.setup();
    clientToken = null;
    storedBlob = null;
    storedGuid = null;
    storedNonce4 = null;

    final To1ClientStorage clientStorage = new To1ClientStorage() {
      @Override
      public Composite getSigInfoA() {
        return cryptoService
            .getSignInfo(
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

        storedBlob = signedBlob;
      }

      @Override
      public Composite getDeviceCredentials() {
        return deviceCreds;
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

    clientService = new To1ClientService() {
      @Override
      protected To1ClientStorage getStorage() {
        return clientStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }
    };

    To1ServerStorage serverStorage = new To1ServerStorage() {
      @Override
      public UUID getGuid() {
        return storedGuid;
      }

      @Override
      public void setGuid(UUID guid) {
        storedGuid = guid;
      }

      @Override
      public byte[] getNonce4() {
        return storedNonce4;
      }

      @Override
      public void setNonce4(byte[] nonce4) {
        storedNonce4 = nonce4;
      }

      @Override
      public Composite getSigInfoB(Composite signInfoA) {
        return signInfoA;
      }

      @Override
      public PublicKey getVerificationKey() {
        return PemLoader.loadCerts(devKeyPem).get(0).getPublicKey();
      }

      @Override
      public Composite getRedirectBlob() {
        return unsignedRedirect;
      }

      @Override
      public void starting(Composite request, Composite reply) {

      }

      @Override
      public void started(Composite request, Composite reply) {
        reply.set(Const.SM_PROTOCOL_INFO,
            Composite.newMap().set(Const.PI_TOKEN, sererToken));
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

    serverService = new To1ServerService() {
      @Override
      public To1ServerStorage getStorage() {
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
    assertTrue(storedBlob != null);

  }
}
