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
import org.fido.iot.certutils.PemLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class To2AsymkexTest extends BaseTemplate {

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

  static String rsaKeyPem = "-----BEGIN RSA PRIVATE KEY-----\n"
      + "MIIEowIBAAKCAQEA0ZLeq8n5bU0gRYhRCpL887sQ8Kl0PZ0GiDO0Cil1tRP3KWKF\n"
      + "4YG5NB7a7G25Y5mC7hfUIogfPHCMgAsViM2JLqWaUD7ZUl8Ky4DgrI2hQDwUfB6/\n"
      + "GONc5cO+82nyLz/hbbII6gM6r3A5mV8UG64Dd/i4QCRYPhZD7LfbMUsWsJelXxsk\n"
      + "uYZ7HFPw2nS4Nbq8XuCfl+0pfFc3w9kJMwel4vyqTi9DfmblR6hy+H8VTxJqjZ+3\n"
      + "xtWxmkQT5TbHwZMgCxR3GSNnzueW1J7tkugTRCXbCFQ6rRyzbkHFpY8VsckdJ2PO\n"
      + "Jp53vxGjJ09kYbTnNOk2hLLaij4KgGfMsPZ2VQIDAQABAoIBAGPmJs6s0IERqrh6\n"
      + "amcn+fwUx9ZwcECOgXabbs8JRFq00bSgikEcnTQDIUhiZWTc4FSudyieyoT9AXxn\n"
      + "zDQSBbp+pjhqPR3OwE6ReIfIW31LJlikL2OlvbqovHU7o35ybPrlSuqB74+BZ3N8\n"
      + "pxEqueyOWwX4TTgLVQWZ/ppYuNAkHEoXR1k7tPNADy/QQ5BSPThkJhof0scYruOn\n"
      + "NsA9pvvQCBiWMjLQB0LHnmICDU1ipK8Beg/QDTAI6jRKXaYSGd6TOxdE++OeqGRS\n"
      + "cw/PzhtKb5DVlTfAjXdRBbw7RNXluIMzAGXkdVWAQv6mhJBCCrJFXSb40piXpDy/\n"
      + "SQgK6WECgYEA89z/+ErnlxsfxeH0Nyc50UIAlTYS6SEyp9MfVY5ceuVxqhtYYT3n\n"
      + "PHQ2LdAIX8453KktlLKVgcQV+hJVprshqQzsCMsfy5CM9FQaGAL6d8i2YHc8QA+x\n"
      + "4hUeePWYGO5fjtjumeT5VQeFExCOZetpsRpO0PTXlGoyv8r7bkmgxksCgYEA3AD+\n"
      + "xo1oS+vKiRyQFDouuugn+ApZ2cSpRB71Z4k6z0OhpsYYJ919SE2WlEWiDD1jYxH1\n"
      + "HHKBHOPU3AbDWamuoUWB99KioCwK8z5qBsboLuT6Thej0/VIKTIUDIeSzruXEMHf\n"
      + "94sYOiuayN7Bffr+F5WnL2BYJamyB3oQvQIEUd8CgYBdn8ZiRBe/yrkbFtHU7uli\n"
      + "ro1cu+LswEMbbZHFHh/hSv+liFQZGVQSwKcgVZMxfRJ35jfKs/l0VZz+YlWh6oxU\n"
      + "w9JG2f0+ASQslbFi8JnKrTNfu8dU7PGlU+hcFiYrxDDJvf109hTHkViE5kFVXdk5\n"
      + "FlmWHbo6j78mJT78q6XfGwKBgCMBlYiKX5oU/rkqblKJn33mCtYQ75MTE8vfETVU\n"
      + "RKNOI56hzp4pRBIJJniZS9ueuP2+kb3hCmJKbkhEWzkdIshOgwun3HCYEXX67Gv/\n"
      + "oloz3RAn+s1zLfRAH6iOufFjL/penwCIhWZcIdjseOVO1rdx/JQxRFVZ1xYRejBY\n"
      + "7c8JAoGBAO6zBcjXtyKs9o3Nvfg1nxmecqoywQeeq8W6Cxqg0TfakSDFqA8O5HGk\n"
      + "1b/3qNwYcpj3KsfNXPcqjf8mr41BWDrxF7p8wTRUSxSfrbSH5UfDp+VVIdF9RiQK\n"
      + "VZHGKvpRI8icbSjwPR7L27FNf1iO8wsGRKKZ+BLHtdsP8vWAz506\n"
      + "-----END RSA PRIVATE KEY-----\n"
      + "-----BEGIN CERTIFICATE-----\n"
      + "MIIDZTCCAk2gAwIBAgIUOfe8MLTzZPyKINfqasJXdYKoKyQwDQYJKoZIhvcNAQEL\n"
      + "BQAwQjELMAkGA1UEBhMCWFgxFTATBgNVBAcMDERlZmF1bHQgQ2l0eTEcMBoGA1UE\n"
      + "CgwTRGVmYXVsdCBDb21wYW55IEx0ZDAeFw0yMDEwMTIxMzE1NTNaFw00ODAyMjcx\n"
      + "MzE1NTNaMEIxCzAJBgNVBAYTAlhYMRUwEwYDVQQHDAxEZWZhdWx0IENpdHkxHDAa\n"
      + "BgNVBAoME0RlZmF1bHQgQ29tcGFueSBMdGQwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n"
      + "DwAwggEKAoIBAQDRkt6ryfltTSBFiFEKkvzzuxDwqXQ9nQaIM7QKKXW1E/cpYoXh\n"
      + "gbk0HtrsbbljmYLuF9QiiB88cIyACxWIzYkupZpQPtlSXwrLgOCsjaFAPBR8Hr8Y\n"
      + "41zlw77zafIvP+FtsgjqAzqvcDmZXxQbrgN3+LhAJFg+FkPst9sxSxawl6VfGyS5\n"
      + "hnscU/DadLg1urxe4J+X7Sl8VzfD2QkzB6Xi/KpOL0N+ZuVHqHL4fxVPEmqNn7fG\n"
      + "1bGaRBPlNsfBkyALFHcZI2fO55bUnu2S6BNEJdsIVDqtHLNuQcWljxWxyR0nY84m\n"
      + "nne/EaMnT2RhtOc06TaEstqKPgqAZ8yw9nZVAgMBAAGjUzBRMB0GA1UdDgQWBBR4\n"
      + "OQr8fvWobpV+YSGhnvYAok0pdzAfBgNVHSMEGDAWgBR4OQr8fvWobpV+YSGhnvYA\n"
      + "ok0pdzAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCQRe4+J0YV\n"
      + "i0ticInMN0eJ6WNUaaFYWMNZUUGUKCE+l0a6WC0D80ZR8Zbik0ZZO6nXyYTrY5KZ\n"
      + "yf//H4WVUt0N5pmslMx8nOFPKmCA9BhAXACnw+YDw1g+pnhvhQbJoXUoFuDBvM/2\n"
      + "4QSVg1Bz5XFlwGOAv5f3SmJWHf9XxUOoIsHvWHSd6JWVAcrBaQuJoAteBe79s2ai\n"
      + "Y99K6IK8tuRczhF7f7Mb6uhHY62jTRZTixX0idY1hvdazzs7s4MZjF3Rwf+Fz2by\n"
      + "k8fdQj4sxMTxH9tStfvwiFvEXVpwTrdaTiAgkki9QQtLJvCynWz/a9V1ImJK8urR\n"
      + "8RyEUcJkOnJw\n"
      + "-----END CERTIFICATE-----\n";

  @BeforeAll
  static void beforeAll() {
    ownerKeyPem = rsaKeyPem;
    mfgKeyPem   = rsaKeyPem;
    devKeyPem   = rsaKeyPem;
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
        return Const.ASYMKEX2048_ALG_NAME;
      }

      @Override
      public String getCipherSuiteName() {
        return Const.AES128_CTR_HMAC256_ALG_NAME;
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
      public PrivateKey getOwnerSigningKey(PublicKey key) {
        return PemLoader.loadPrivateKey(rsaKeyPem);
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

      @Override
      public void setReplacementHmac(byte[] hmac) {

      }

      @Override
      public boolean getOwnerResaleSupport() {
        return false;
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
