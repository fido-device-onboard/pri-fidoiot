// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;
import org.junit.jupiter.api.Test;

public class To2Ccm128Test extends BaseTemplate {

  private String serverToken = guid.toString();
  private String clientToken;
  private byte[] storedNonceTo2ProveDv;
  private byte[] storedNonceTo2SetupDv;
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
    storedNonceTo2ProveDv = null;
    storedNonceTo2SetupDv = null;
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
        return Const.ECDH256_ALG_NAME;
      }

      @Override
      public String getCipherSuiteName() {
        return Const.AES_CCM_64_128_128_ALG_NAME;
      }

      @Override
      public byte[] getReplacementHmacSecret(Composite newCredentials, boolean isReuse) {
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
      public void setMaxDeviceServiceInfoMtuSz(int mtu) {
        prepareServiceInfo();
      }

      @Override
      public int getMaxDeviceServiceInfoMtuSz() {
        return Const.DEFAULT_SERVICE_INFO_MTU_SIZE;
      }

      @Override
      public String getMaxOwnerServiceInfoMtuSz() {
        return String.valueOf(Const.DEFAULT_SERVICE_INFO_MTU_SIZE);
      }

      @Override
      public boolean isDeviceCredReuseSupported() {
        return true;
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

    To2ClientService to2Service = new To2ClientService() {
      @Override
      protected To2ClientStorage getStorage() {
        return clientStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }
    };

    to2Service.setTo1d(generateSignedBlob(ownerKeyPem));
    clientService = to2Service;

    To2ServerStorage serverStorage = new To2ServerStorage() {
      @Override
      public PrivateKey getOwnerSigningKey(PublicKey key) {
        return PemLoader.loadPrivateKey(ownerKeyPem);
      }

      @Override
      public byte[] getNonceTo2ProveDv() {
        return storedNonceTo2ProveDv;
      }

      @Override
      public void setNonceTo2ProveDv(byte[] nonce) {
        storedNonceTo2ProveDv = nonce;
      }

      @Override
      public byte[] getNonceTo2SetupDv() {
        return storedNonceTo2SetupDv;
      }

      @Override
      public void setNonceTo2SetupDv(byte[] nonce) {
        storedNonceTo2SetupDv = nonce;
      }

      @Override
      public OnDieService getOnDieService() { return null; }

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
      public UUID getGuid() { return storedGuid; }

      @Override
      public Composite getVoucher() {
        assertTrue(storedGuid != null);
        return voucher;
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
      public void setReplacementHmac(byte[] hmac) {

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

      @Override
      public void storeVoucher(Composite voucher) {
      }

      @Override
      public void discardReplacementOwnerKey() {
      }

      @Override
      public byte[] getReplacementHmac() {
        return null;
      }

      public Composite getSigInfoA() {
        return null;
      }

      @Override
      public boolean getOwnerResaleSupport() {
        return true;
      }

      @Override
      public String getMaxDeviceServiceInfoMtuSz() {
        return String.valueOf(Const.DEFAULT_SERVICE_INFO_MTU_SIZE);
      }

      @Override
      public void setMaxOwnerServiceInfoMtuSz(int mtu) {

      }

      @Override
      public int getMaxOwnerServiceInfoMtuSz() {
        return Const.DEFAULT_SERVICE_INFO_MTU_SIZE;
      }

      public void setSigInfoA(Composite sigInfoA) {
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
