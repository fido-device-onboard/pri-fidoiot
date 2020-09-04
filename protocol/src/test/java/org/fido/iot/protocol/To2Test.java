// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.fido.iot.certutils.PemLoader;

public class To2Test extends BaseTemplate {

  private String serverToken = guid.toString();
  private String clientToken;
  private byte[] storedNonce6;
  private byte[] storedNonce7;
  private Composite storedOwnerState;
  private String storedCipherName;
  private UUID storedGuid;

  Composite toOwnerInfo;
  Composite toDeviceInfo;

  Composite fromOwnerInfo;
  Composite fromDeviceInfo;

  @Override
  protected void setup() throws Exception {

    clientToken = null;
    storedNonce6 = null;
    storedNonce7 = null;
    storedOwnerState = null;
    storedCipherName = null;
    storedGuid = null;
    toDeviceInfo = null;
    toOwnerInfo = null;
    fromDeviceInfo = null;
    fromOwnerInfo = null;

    super.setup();

    final To2ClientStorage clientStorage = new To2ClientStorage() {

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
        fromOwnerInfo = info;
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

    clientService = new To2ClientService() {
      @Override
      protected To2ClientStorage getStorage() {
        return clientStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }
    };

    To2ServerStorage serverStorage = new To2ServerStorage() {
      @Override
      public PrivateKey geOwnerSigningKey(PublicKey key) {
        return PemLoader.loadPrivateKey(ownerKeyPem);
      }

      @Override
      public byte[] getNonce6() {
        return storedNonce6;
      }

      @Override
      public void setNonce6(byte[] nonce) {
        storedNonce6 = nonce;
      }

      @Override
      public byte[] getNonce7() {
        return storedNonce7;
      }

      @Override
      public void setNonce7(byte[] nonce) {
        storedNonce7 = nonce;
      }

      @Override
      public void setOwnerState(Composite ownerState) {
        storedOwnerState = ownerState;
      }

      @Override
      public Composite getOwnerState() {
        return storedOwnerState;
      }

      @Override
      public void setCipherName(String cipherName) {

        storedCipherName = cipherName;
      }

      @Override
      public String getCipherName() {
        return storedCipherName;
      }

      @Override
      public void setGuid(UUID guid) {
        storedGuid = guid;
      }

      @Override
      public Composite getVoucher() {
        assertTrue(storedGuid != null);
        return voucher;
      }

      @Override
      public Composite getSigInfoB(Composite sigInfoA) {
        return sigInfoA;
      }

      @Override
      public Composite getReplacementRvInfo() {
        return null;
      }

      @Override
      public UUID getReplacementGuid() {
        return null;
      }

      @Override
      public Composite getReplacementOwnerKey() {
        return null;
      }

      @Override
      public void setReplacementHmac(Composite hmac) {

      }

      @Override
      public void prepareServiceInfo() {
        Composite value = ServiceInfoEncoder.encodeValue("sysmod:active", "true");
        List<Composite> list = new ArrayList<>();
        list.add(value);
        toDeviceInfo = ServiceInfoEncoder.encodeOwnerServiceInfo(list, false, false);
      }

      @Override
      public Composite getNextServiceInfo() {

        Composite result = toDeviceInfo;
        toDeviceInfo = ServiceInfoEncoder.encodeOwnerServiceInfo(
            Collections.EMPTY_LIST, false, true);

        return result;
      }

      @Override
      public void setServiceInfo(Composite info, boolean isMore) {
        fromDeviceInfo = info;
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

    serverService = new To2ServerService() {
      @Override
      public To2ServerStorage getStorage() {
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
    assertTrue(fromDeviceInfo != null);
    assertTrue(fromOwnerInfo != null);
  }

}
