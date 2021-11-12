// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.DispatchResult;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.RendezvousBlobDecoder;
import org.fidoalliance.fdo.protocol.ServiceInfoEncoder;
import org.fidoalliance.fdo.protocol.To2ClientService;
import org.fidoalliance.fdo.protocol.To2ClientStorage;
import org.fidoalliance.fdo.serviceinfo.DevMod;
import org.fidoalliance.fdo.serviceinfo.FdoSys;

public class To2ClientApp {

  private static final LoggerService logger = new LoggerService(To2ClientApp.class);
  private static final int RV_PORT = 8040;
  private static final String HOST_NAME = "localhost";
  private static final String PROTOCOL_NAME = "http://";

  static final String devKeyPem = "-----BEGIN CERTIFICATE-----\n"
      + "MIIBdjCCAR0CCQCNo1W35xxR9TAKBggqhkjOPQQDAjANMQswCQYDVQQDDAJDQTAg\n"
      + "Fw0xOTExMjIxNTU0MDFaGA8yMDU0MTExMzE1NTQwMVoweDELMAkGA1UEBhMCVVMx\n"
      + "DzANBgNVBAgMBk9yZWdvbjESMBAGA1UEBwwJSGlsbHNib3JvMQ4wDAYDVQQKDAVJ\n"
      + "bnRlbDEdMBsGA1UECwwURGV2aWNlIE1hbnVmYWN0dXJpbmcxFTATBgNVBAMMDERl\n"
      + "bW9EZXZpY2UyNzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABKWC8HLsakdG2OfJ\n"
      + "dFWKbE7GlM6RQgqXjd25ldIB6ecSxzMLwRUcjrZWMTdF2sfHBA7H7yLlSWIWMrWz\n"
      + "hj5GfJgwCgYIKoZIzj0EAwIDRwAwRAIgQ4YHfzmu55T35I6vBP9MGIIqjDBplK1K\n"
      + "11zKta73R4wCIHPOGDQpRSZiwp1MTRt1D2MWfoXJyw73slgamG7JKCvx\n"
      + "-----END CERTIFICATE-----\n"
      + "-----BEGIN EC PARAMETERS-----\n"
      + "BggqhkjOPQMBBw==\n"
      + "-----END EC PARAMETERS-----\n"
      + "-----BEGIN EC PRIVATE KEY-----\n"
      + "MHcCAQEEIH5hKR4Yit57JC0SVpIyAUtrHnnHcYEzDHLrs5ogHWgtoAoGCCqGSM49\n"
      + "AwEHoUQDQgAEpYLwcuxqR0bY58l0VYpsTsaUzpFCCpeN3bmV0gHp5xLHMwvBFRyO\n"
      + "tlYxN0Xax8cEDsfvIuVJYhYytbOGPkZ8mA==\n"
      + "-----END EC PRIVATE KEY-----";

  private static final String deviceCreds = ""
      + "87f51864582054686973206973206120534841323536206b657920666f7220686d616320616c6a44656d6f446"
      + "5766963655086a8f055ac1d4a5cb491e250f4fec1b081858205696c6f63616c686f73748203191f68820c0182"
      + "02447f0000018204191f69822f582032f840822e21d4b8a5ee1bf66bb3a08ead5f5f7e4a086576945447e46cb"
      + "fae8f";

  private static final String rendezvousBlob = ""
      + "d28443a10126a0583a828184447f000001696c6f63616c686f7374191f6a03822f5820e77fae3bd6a1f592442"
      + "ffe9ee54274d69fb2aea7fa277694a5db4065b07aca0158400c92a4cb4e04a65f122d1319b8c43b4ef6a49e85"
      + "7c9f88c38e1c145b99afbf9da553dbb123ece759c844b51eee0a724dc812817f8a004be3a8cc6960e84e63b0";

  private CryptoService cryptoService;
  private Composite to1dBlob = Composite.fromObject(rendezvousBlob);

  private void run(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {

    cryptoService = new CryptoService();

    MessageDispatcher dispatcher = getDispatcher();

    DispatchResult dr = clientService.getHelloMessage();

    clientService.setTo1d(to1dBlob);

    List<String> paths = RendezvousBlobDecoder.getHttpDirectives(to1dBlob);

    for (String path : paths) {

      try {
        WebClient client = new WebClient(path, dr, dispatcher);
        client.call();
        // break here since no exception is thrown, so TO2 is successful.
        break;
      } catch (Exception e) {
        logger.error(e);
      }
    }

  }

  private To2ClientStorage clientStorage = new To2ClientStorage() {
    private String clientToken;
    private Composite toOwnerInfo;
    int maxServiceInfoSize = 0;

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

    @Override
    public Composite getDeviceCredentials() {
      return Composite.fromObject(deviceCreds);
    }

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
      return Const.AES128_CTR_HMAC256_ALG_NAME;
    }

    @Override
    public byte[] getReplacementHmacSecret(Composite newCredentials, boolean isReuse) {
      if (isReuse) {
        return null;
      } else {
        // Defaulting to 32-bytes key length for HMAC-SHA-256 that this device uses.
        return cryptoService.getRandomBytes(256 / 8);
      }
    }

    @Override
    public void prepareServiceInfo() {
      List<Composite> list = new ArrayList<>();

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_ACTIVE, true));
      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_OS, "linux"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_ARCH, "X86_64"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_BIN, "x86:X86_64"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_VERSION, "Ubuntu* 16.0.4LTS"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_DEVICE, "ProtocolDevice"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_SN, "AABCCDDDEEF"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_PATHSEP, "/"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_SEP, ":"));
      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_NL, "\n"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_TMP, "/tmp"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_DIR, "/home/fdo"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_PROGENV, "bin:java:py3:py2"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_MUDURL, "https://example.com/devB"));

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_NUMMODULES, 1));

      Composite modules = Composite.newArray()
          .set(0, 0) //zero to num module index
          .set(1, 1) // the number returned
          .set(2, FdoSys.NAME); //the module

      list.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_MODULES, modules));

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

      String name = info.getAsString(Const.FIRST_KEY);
      Object value = info.get(Const.SECOND_KEY);
      logger.info(name);
      logger.info("=");
      if (value instanceof String
          || value instanceof Boolean
          || value instanceof Number) {
        logger.info(value.toString());
      } else if (value instanceof byte[]) {
        logger.info(Composite.toString((byte[]) value));
      } else if (value instanceof Map || value instanceof List) {
        Composite composite = info.getAsComposite(Const.SECOND_KEY);
        logger.info(composite.toString());
      }
      logger.info("");
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
  };

  To2ClientService clientService = new To2ClientService() {
    @Override
    protected To2ClientStorage getStorage() {
      return clientStorage;
    }

    @Override
    public CryptoService getCryptoService() {
      return cryptoService;
    }
  };

  private MessageDispatcher getDispatcher() {

    return new MessageDispatcher() {

      @Override
      protected MessagingService getMessagingService(Composite request) {
        return clientService;
      }

      @Override
      protected void failed(Exception e) {
        logger.error(e.getMessage());
      }
    };
  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {
    new To2ClientApp().run(args);
    logger.info("TO2 Client finished.");
    return;
  }
}
