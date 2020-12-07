// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.fido.iot.certutils.PemLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class To2EcRsaTest extends BaseTemplate {

  static String EC384_PEM = "-----BEGIN CERTIFICATE-----\n"
      + "MIICyzCCATMCAhI1MA0GCSqGSIb3DQEBCwUAMEIxCzAJBgNVBAYTAlhYMRUwEwYD\n"
      + "VQQHDAxEZWZhdWx0IENpdHkxHDAaBgNVBAoME0RlZmF1bHQgQ29tcGFueSBMdGQw\n"
      + "HhcNMjAxMTEzMTcyMTE4WhcNNDgwMzMwMTcyMTE4WjBCMQswCQYDVQQGEwJYWDEV\n"
      + "MBMGA1UEBwwMRGVmYXVsdCBDaXR5MRwwGgYDVQQKDBNEZWZhdWx0IENvbXBhbnkg\n"
      + "THRkMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEJp3yeoK1BkkAtrlkcYLBQLjPDjO2\n"
      + "7fhmF+W8M7CQCsujp3sLH/ETWvm6Cb1PcVM+piOGZmylgqk8gsFPYawqZ994F5TC\n"
      + "DKWxvGiVEPtbfc/BMXDN7pHzCR22A6N5/U0dMA0GCSqGSIb3DQEBCwUAA4IBgQAL\n"
      + "kjvK9PPuDRIEYPc0RtQz387d25E7zcsSb0O9iTEZqaTGY5ngo19QYZ1w+ab7OByk\n"
      + "bJomcBrqvsfpuJUAUfb7udLhPV/TrfFQxDINN9z4QMTpKuzMdg3fYK6C+xLOcPcz\n"
      + "w9ipq9f/lG6aDl0vm1COBJ3Y2Mz9NS8zQjqT9Nb6SQ3TiFX9C7E/9hSIgYFkPI7N\n"
      + "jkml4O+jH4nbBlvmm6H7DPj3Ss+dTau3TFo3JqmfIN3MP32JUD7T1dShTmV4fapV\n"
      + "iGBrdAwW2FfgzOSPSK2y/HE0XR93vlYImdQyBWBL8I4Li3F/N03kN5CPcR4pQEo5\n"
      + "knElZGAkQ7xL7CKM2INAGDBshITGq3rjLKwHDZHDxonvc0N3pQDAnhyKtorgJo0O\n"
      + "U7oGkaZ4S3tdW28Pg0cWfZT9ITMHs1CHS9vtAmRhtUu7+ZAD9bqhVty1ky8mmlf6\n"
      + "V5RU5tN9vxBTlaE8c7kt6u7TJzIluHOcNpUu/x41YNTL5xt7d5JurIuGdMNqWR0=\n"
      + "-----END CERTIFICATE-----\n"
      + "-----BEGIN EC PRIVATE KEY-----\n"
      + "MIGkAgEBBDCPwfsoUfDZMsInbdPRF+5aJDg0CsCET4a8AGz4JhBcxRVqvTdVZCbV\n"
      + "dfBygsgV1hCgBwYFK4EEACKhZANiAAQmnfJ6grUGSQC2uWRxgsFAuM8OM7bt+GYX\n"
      + "5bwzsJAKy6Onewsf8RNa+boJvU9xUz6mI4ZmbKWCqTyCwU9hrCpn33gXlMIMpbG8\n"
      + "aJUQ+1t9z8ExcM3ukfMJHbYDo3n9TR0=\n"
      + "-----END EC PRIVATE KEY-----\n";
  static String RSA3K_PEM = "-----BEGIN CERTIFICATE-----\n"
      + "MIIEZTCCAs2gAwIBAgIUWnL37SpsXjf0j4OpnsvGgkiYtyUwDQYJKoZIhvcNAQEL\n"
      + "BQAwQjELMAkGA1UEBhMCWFgxFTATBgNVBAcMDERlZmF1bHQgQ2l0eTEcMBoGA1UE\n"
      + "CgwTRGVmYXVsdCBDb21wYW55IEx0ZDAeFw0yMDExMTExNTQwMDFaFw00ODAzMjgx\n"
      + "NTQwMDFaMEIxCzAJBgNVBAYTAlhYMRUwEwYDVQQHDAxEZWZhdWx0IENpdHkxHDAa\n"
      + "BgNVBAoME0RlZmF1bHQgQ29tcGFueSBMdGQwggGiMA0GCSqGSIb3DQEBAQUAA4IB\n"
      + "jwAwggGKAoIBgQCsKpBj2+ixpECkffwoUJtSDWJBNgb4CdQs9sSGr+Htz8kUPSS3\n"
      + "2NAR1OXRB2fQY0hSB2uWDoiayoZLVCSsMA4/HK+2nJUUDgn62/HyQ/uOF8u2H7Jh\n"
      + "9Wpo5Po0OPuiOIjwp+PBdPXO1CjJheUrYVwj7UWRlnlzpyE/GthR9CJI8YWHleB6\n"
      + "+IDYqjzUsd51Q+fpWJQP0p0/0yKzVhevoVKF/1WthhsNo4334BUf9k7dfV53XUlA\n"
      + "cFLdXh3N9NGFPICxNUOsLJI64c2oG2lS7GFSB4jb+6d/F1G0KXrVIr1zcnMduMyi\n"
      + "o6YqrWveqqI3bL6jljD6CMrQMaa0FtYSEBN5tvxqjwS+McTT4QaXetyfxG7PO6u+\n"
      + "bRt6HL6PXpxb/DVGGcHW3XxpAnzr9EXaRgwKUHDyGTva3akjmwB/o5lfyZRVeE1v\n"
      + "pkJ1xXdBzWVtQ2bo8QGshqD27oIwnzjXXqdSiC4SqRUaM2OtsxUBgQB91aRj8LWz\n"
      + "hz9JjB8eAeI92XMCAwEAAaNTMFEwHQYDVR0OBBYEFKB0A8X0WO2LYzxPVPZGdRiU\n"
      + "FdqNMB8GA1UdIwQYMBaAFKB0A8X0WO2LYzxPVPZGdRiUFdqNMA8GA1UdEwEB/wQF\n"
      + "MAMBAf8wDQYJKoZIhvcNAQELBQADggGBAABohM+EipDH7uU/ufTsK1JwOYOkI0Kj\n"
      + "noa2TFHyt6QOupDR5c/R/ofhSkpEgX0qpjJL0fvJ+5fE93FUXbyYX5Ae1kbFaj77\n"
      + "/JkYDgT/PpqoypJdHyQ+W+OrojaRtFdY9TvlFjxwhG5MzmveGJw+LljMbZXoy0kn\n"
      + "JOXsCYZELG3E0/lua2JaQDWk/KWu7X554Px630lxog8jrzL6sndmgbtstxDlPupE\n"
      + "x+ujFV1LnpmmduIe+nMWCI6GYCohK0vatVuFk2MsrTzXr7NXFZE1NaFXQuUecYgD\n"
      + "7xBtmWK4KyvYPWR7QPESDNh9dmt9HQyg1RHyWECGAjGtUiQds2wwyUmznW6L/alq\n"
      + "NFxt07guSqttMmOrpuTY7/1FVf48Oj2msEUcFTr2WtpZG5+/O5fhO+31Q5NNsOnv\n"
      + "8EIEPeYZrXANcB1va/s0wKpTeiKMHrgmpexngsNTFjCdgtbw+enbATmdJ04kAlCF\n"
      + "NA/rzYvcOYYMVx6G4GdKwPariFiVyLQwCQ==\n"
      + "-----END CERTIFICATE-----\n"
      + "-----BEGIN RSA PRIVATE KEY-----\n"
      + "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\n"
      + "-----END RSA PRIVATE KEY-----\n";
  Composite toOwnerInfo;
  Composite toDeviceInfo;
  Composite fromOwnerInfo;
  Composite fromDeviceInfo;
  private final String serverToken = guid.toString();
  private String clientToken;
  private byte[] storedNonce6;
  private byte[] storedNonce7;
  private Composite storedOwnerState;
  private String storedCipherName;
  private UUID storedGuid;

  @BeforeAll
  static void beforeAll() {
    Security.addProvider(CryptoService.BCPROV);
    ownerKeyPem = RSA3K_PEM;
    mfgKeyPem = RSA3K_PEM;
    devKeyPem = EC384_PEM;
  }

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
      public void continuing(Composite request, Composite reply) {
        Composite info = request.getAsComposite(Const.SM_PROTOCOL_INFO);
        if (info.containsKey(Const.PI_TOKEN)) {
          clientToken = info.getAsString(Const.PI_TOKEN);
        }
        reply.set(Const.SM_PROTOCOL_INFO,
            Composite.newMap().set(Const.PI_TOKEN, clientToken));
      }

      @Override
      public String getCipherSuiteName() {
        return Const.AES256_CTR_HMAC384_ALG_NAME;
      }

      @Override
      public Composite getDeviceCredentials() {
        return deviceCreds;
      }

      @Override
      public String getKexSuiteName() {
        return Const.ASYMKEX3072_ALG_NAME;
      }

      @Override
      public byte[] getMaroePrefix() {
        return null;
      }

      @Override
      public Composite getNextServiceInfo() {
        Composite result = toOwnerInfo;
        toOwnerInfo = ServiceInfoEncoder.encodeDeviceServiceInfo(
            Collections.EMPTY_LIST, false);
        return result;
      }

      @Override
      public byte[] getReplacementHmacSecret(Composite newCredentials, boolean isReuse) {
        return null;
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
      public PrivateKey getSigningKey() {
        return PemLoader.loadPrivateKey(devKeyPem);
      }

      @Override
      public void prepareServiceInfo() {
        Composite value = ServiceInfoEncoder.encodeValue("devmod:active", "true");
        List<Composite> list = new ArrayList<>();
        list.add(value);
        toOwnerInfo = ServiceInfoEncoder.encodeDeviceServiceInfo(list, false);
      }

      @Override
      public void setServiceInfo(Composite info, boolean isMore, boolean isDone) {
        fromOwnerInfo = info;
      }
    };

    clientService = new To2ClientService() {
      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }

      @Override
      protected To2ClientStorage getStorage() {
        return clientStorage;
      }
    };

    To2ServerStorage serverStorage = new To2ServerStorage() {
      @Override
      public void discardReplacementOwnerKey() {

      }

      @Override
      public String getCipherName() {
        return storedCipherName;
      }

      @Override
      public void setCipherName(String cipherName) {

        storedCipherName = cipherName;
      }

      @Override
      public Composite getNextServiceInfo() {

        Composite result = toDeviceInfo;
        toDeviceInfo = ServiceInfoEncoder.encodeOwnerServiceInfo(
            Collections.EMPTY_LIST, false, true);

        return result;
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
      public boolean getOwnerResaleSupport() {
        return false;
      }

      @Override
      public PrivateKey getOwnerSigningKey(PublicKey key) {
        return PemLoader.loadPrivateKey(RSA3K_PEM);
      }

      @Override
      public Composite getOwnerState() {
        return storedOwnerState;
      }

      @Override
      public void setOwnerState(Composite ownerState) {
        storedOwnerState = ownerState;
      }

      @Override
      public UUID getReplacementGuid() {
        return null;
      }

      @Override
      public byte[] getReplacementHmac() {
        return null;
      }

      @Override
      public void setReplacementHmac(byte[] hmac) {

      }

      @Override
      public Composite getReplacementOwnerKey() {
        return null;
      }

      @Override
      public Composite getReplacementRvInfo() {
        return null;
      }

      @Override
      public Composite getSigInfoA() {
        return null;
      }

      @Override
      public void setSigInfoA(Composite sigInfoA) {
      }

      @Override
      public Composite getVoucher() {
        assertTrue(storedGuid != null);
        return voucher;
      }

      @Override
      public void prepareServiceInfo() {
        Composite value = ServiceInfoEncoder.encodeValue("sysmod:active", "true");
        List<Composite> list = new ArrayList<>();
        list.add(value);
        toDeviceInfo = ServiceInfoEncoder.encodeOwnerServiceInfo(list, false, false);
      }

      @Override
      public void setGuid(UUID guid) {
        storedGuid = guid;
      }

      @Override
      public void setServiceInfo(Composite info, boolean isMore) {
        fromDeviceInfo = info;
      }

      @Override
      public void started(Composite request, Composite reply) {
        reply.set(Const.SM_PROTOCOL_INFO,
            Composite.newMap().set(Const.PI_TOKEN, serverToken));
      }

      @Override
      public void storeVoucher(Composite voucher) {

      }
    };

    serverService = new To2ServerService() {
      @Override
      public CryptoService getCryptoService() {
        return cryptoService;
      }

      @Override
      public To2ServerStorage getStorage() {
        return serverStorage;
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
