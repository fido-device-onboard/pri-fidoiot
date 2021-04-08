// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.protocol.ondie.OnDieCache;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;
import org.h2.tools.Server;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
import org.junit.jupiter.api.Test;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.protocol.RendezvousBlobDecoder;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.DispatchException;
import org.fidoalliance.fdo.protocol.DispatchResult;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.To0ClientService;
import org.fidoalliance.fdo.protocol.To0ClientStorage;
import org.fidoalliance.fdo.protocol.To0ServerService;
import org.fidoalliance.fdo.protocol.To0ServerStorage;
import org.fidoalliance.fdo.protocol.To1ClientService;
import org.fidoalliance.fdo.protocol.To1ClientStorage;
import org.fidoalliance.fdo.protocol.To1ServerService;
import org.fidoalliance.fdo.protocol.To1ServerStorage;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(Alphanumeric.class)
public class RvsStorageTest {

  private static final String DB_HOST = "localhost";
  private static final String DB_PORT = "8043";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private static final String RV_BLOB = "http://localhost:8040?ipaddress=127.0.0.1";

  private static final String VOUCHER = "8486186450994f1a570a694fdc8ad761e094ef7f7481858205696c6f6"
      + "3616c686f73748203191f68820c018202447f00000182041920fb6b4a6176612044657669636583260258402c"
      + "02709032b3fc1696ab55b1ecf8e44795b92cb21b6a681265e54d525c8533fb74b0c0310166ef11b0f32aef76e"
      + "135f86acdd65633267de932b31df43e50c625822f5820b2daf2d185dd294d47fce206ae5b460aea3c09535d03"
      + "bdc1c6b45b2a3b3208a2820558207d3cc4e0d6740f04b241e086fa08fbbcc6992b43454fa63ae1f3d5eb7ca30"
      + "7cc825901013081fe3081a5a00302010202060178afa1ca42300a06082a8648ce3d040302300d310b30090603"
      + "5504030c024341301e170d3231303430383033353833395a170d3331303430363033353833395a30003059301"
      + "306072a8648ce3d020106082a8648ce3d03010703420004a582f072ec6a4746d8e7c974558a6c4ec694ce9142"
      + "0a978dddb995d201e9e712c7330bc1151c8eb656313745dac7c7040ec7ef22e549621632b5b3863e467c98300"
      + "a06082a8648ce3d040302034800304502204598864ee60e7604f950dd73123623d13b6c21a6faa5d93b2b49ce"
      + "a2d2787531022100fc86d2ec647faca94088891a8b3f2fd2e19c5966179a98c77c17d6690f051d74590126308"
      + "201223081c9a003020102020900a4d303ae980f53f1300a06082a8648ce3d040302300d310b30090603550403"
      + "0c0243413020170d3139303432343134343634375a180f32303534303431353134343634375a300d310b30090"
      + "6035504030c0243413059301306072a8648ce3d020106082a8648ce3d030107034200042c02709032b3fc1696"
      + "ab55b1ecf8e44795b92cb21b6a681265e54d525c8533fb74b0c0310166ef11b0f32aef76e135f86acdd656332"
      + "67de932b31df43e50c625a310300e300c0603551d13040530030101ff300a06082a8648ce3d04030203480030"
      + "45022100a5419b823613d24eb701e440b4f3368be5675ba72461a272bc52eeb96c3e414002204e70d27b631cb"
      + "6efc26aa0c027e1e53eaef1ec5074203683d1ecbb9de129c692818443a10126a0588e83822f582050672aa00c"
      + "7b1d507ca7c7035a345916896b5895a0445f64becab34ff1c62969822f582081ef19ef232e6b13969aac6c64e"
      + "fa52aa9cc319dd239928ebc522728140b8dc18326025840595504d86d062f2f2c72600ec90ca1701885fdf494"
      + "7778bf3a0ed70d286225bd88b1b099491aadd5e935e486de088e73ec11de6b61991a068aeb77320f5e6034584"
      + "73045022100e1cade1a331df0667b01c0f369079bd05f48228925a0c6b1715adae472a4b71702200dc1ea02a3"
      + "7e3242ad23a56d5caf02812d984062717d7f72c8389acd5c1d271f";

  protected static String deviceCreds = ""
      + "87f5186458405c81aba59736cde90097011e02131cf142e721ad166e8eb5f4370d6d52d4d9b33aeebece1bdca"
      + "da81cf6b30acd7e6e80f312d68062050699f47b8c1b9655d1d66b4a6176612044657669636550994f1a570a69"
      + "4fdc8ad761e094ef7f7481858205696c6f63616c686f73748203191f68820c018202447f00000182041920fb8"
      + "22f58209172fd0feaa577bb039d321d9011fe4f10959becbf5adab1d4cbcbb6fce67877";

  protected static String ownerKeyPem = "-----BEGIN CERTIFICATE-----\n"
      + "MIIB9DCCAZmgAwIBAgIJANpFH5JBylZhMAoGCCqGSM49BAMCMGoxJjAkBgNVBAMM\n"
      + "HVNkbyBEZW1vIE93bmVyIFJvb3QgQXV0aG9yaXR5MQ8wDQYDVQQIDAZPcmVnb24x\n"
      + "EjAQBgNVBAcMCUhpbGxzYm9ybzELMAkGA1UEBhMCVVMxDjAMBgNVBAoMBUludGVs\n"
      + "MCAXDTE5MTAxMDE3Mjk0NFoYDzIwNTQxMDAxMTcyOTQ0WjBqMSYwJAYDVQQDDB1T\n"
      + "ZG8gRGVtbyBPd25lciBSb290IEF1dGhvcml0eTEPMA0GA1UECAwGT3JlZ29uMRIw\n"
      + "EAYDVQQHDAlIaWxsc2Jvcm8xCzAJBgNVBAYTAlVTMQ4wDAYDVQQKDAVJbnRlbDBZ\n"
      + "MBMGByqGSM49AgEGCCqGSM49AwEHA0IABFlVBNhtBi8vLHJgDskMoXAYhf30lHd4\n"
      + "vzoO1w0oYiW9iLGwmUkardXpNeSG3giOc+wR3mthmRoGiut3Mg9eYDSjJjAkMBIG\n"
      + "A1UdEwEB/wQIMAYBAf8CAQEwDgYDVR0PAQH/BAQDAgIEMAoGCCqGSM49BAMCA0kA\n"
      + "MEYCIQDrb3b3tigiReIsF+GiImVKJuBsjU6z8mOtlNyfAr7LPAIhAPOl6TaXaasL\n"
      + "vgML12FQQDT502S6PQPxmB1tRrV2dp8/\n"
      + "-----END CERTIFICATE-----\n"
      + "-----BEGIN EC PRIVATE KEY-----\n"
      + "MHcCAQEEIHg45vhXH9m2SdzNxU55cp94yb962JoNn8F9Zpe6zTNqoAoGCCqGSM49\n"
      + "AwEHoUQDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSUd3i/Og7XDShiJb2IsbCZSRqt\n"
      + "1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
      + "-----END EC PRIVATE KEY-----";

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

  private static final String BASE_PATH = Path.of(
      System.getProperty("user.dir"),
      "target", "data",
      "rvs").toString();

  static BasicDataSource ds = new BasicDataSource();
  static {

    ds.setUrl("jdbc:h2:tcp://" + DB_HOST + ":" + DB_PORT + "/" + BASE_PATH.toString());
    ds.setDriverClassName("org.h2.Driver");
    ds.setUsername(DB_USER);
    ds.setPassword(DB_PASSWORD);

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);
  }

  private To1ServerService createTo1Service(CryptoService cs, DataSource ds, OnDieService ods) {
    return new To1ServerService() {
      private To1ServerStorage storage;

      @Override
      public To1ServerStorage getStorage() {
        if (storage == null) {
          storage = new To1DbStorage(cs, ds, ods);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }

  private To0ServerService createTo0Service(CryptoService cs,
                                            DataSource ds) {
    return new To0ServerService() {
      private To0ServerStorage storage;

      @Override
      public To0ServerStorage getStorage() {
        if (storage == null) {
          storage = new To0DbStorage(cs, ds);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }

  @Test
  void importAllowDenyListTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    try {

      server = Server.createTcpServer(args).start();

      RvsDbManager rvsDbManager = new RvsDbManager();

      assertDoesNotThrow( ()-> {
            rvsDbManager.createAllowListDenyListTables(ds);
            rvsDbManager.importGuidFromDenyList(ds);
            rvsDbManager.importAllowListKeyHash(ds);
            rvsDbManager.importDenyListKeyHash(ds);
          });

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }

  }


  @Test
  void to0AllowListDenyListDbStorageTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    try {
      server = Server.createTcpServer(args).start();
      CryptoService cs = new CryptoService();
      To0AllowListDenyListDbStorage storage = new To0AllowListDenyListDbStorage(cs, ds);
      long waitSec = storage.storeRedirectBlob(
          Composite.fromObject(VOUCHER), 1000, new byte[10]);
      assertTrue(waitSec > 0);

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }

  }

  @Test
  void accTest() throws Exception {

    BasicDataSource ds = new BasicDataSource();
    OnDieCache odc = new OnDieCache(URI.create(""), true, "", null);
    OnDieService ods = new OnDieService(odc, false);

    ds.setUrl("jdbc:h2:tcp://" + DB_HOST + ":" + DB_PORT + "/" + BASE_PATH);
    ds.setDriverClassName("org.h2.Driver");
    ds.setUsername(DB_USER);
    ds.setPassword(DB_PASSWORD);

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);
    CryptoService cs = new CryptoService();

    To0ClientStorage to0ClientStorage = new To0ClientStorage() {
      @Override
      public void starting(Composite request, Composite reply) {

      }

      @Override
      public void started(Composite request, Composite reply) {

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
      public Composite getVoucher() {
        return Composite.fromObject(VOUCHER);
      }

      @Override
      public Composite getRedirectBlob() {
        return RendezvousBlobDecoder.decode(RV_BLOB);
      }

      @Override
      public long getRequestWait() {
        return 3600;
      }

      @Override
      public void setResponseWait(long wait) {
        System.out.println("To0 Wait: " + wait + " seconds");
      }

      @Override
      public PrivateKey getOwnerSigningKey(PublicKey ownerKey) {
        return PemLoader.loadPrivateKey(ownerKeyPem);
      }
    };

    To1ClientStorage to1ClientStorage = new To1ClientStorage() {
      @Override
      public void starting(Composite request, Composite reply) {

      }

      @Override
      public void started(Composite request, Composite reply) {

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
      public Composite getDeviceCredentials() {
        return Composite.fromObject(deviceCreds);
      }

      @Override
      public PrivateKey getSigningKey() {
        return PemLoader.loadPrivateKey(devKeyPem);
      }

      @Override
      public Composite getSigInfoA() {
        return cs.getSignInfo(
            PemLoader.loadCerts(devKeyPem)
                .get(0)
                .getPublicKey());
      }

      @Override
      public byte[] getMaroePrefix() {
        return null;
      }

      @Override
      public void storeSignedBlob(Composite signedBlob) {
        System.out.println("To1d" + signedBlob.toString());
      }
    };

    To1ClientService to1ClientService = new To1ClientService() {
      @Override
      protected To1ClientStorage getStorage() {
        return to1ClientStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };

    To0ClientService to0ClientService = new To0ClientService() {
      @Override
      protected To0ClientStorage getStorage() {
        return to0ClientStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };

    MessageDispatcher clientDispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
          case Const.TO0_HELLO_ACK:
          case Const.TO0_ACCEPT_OWNER:
            return to0ClientService;
          case Const.TO1_HELLO_RV_ACK:
          case Const.TO1_RV_REDIRECT:
            return to1ClientService;
          default:
            throw new DispatchException(new IllegalArgumentException());
        }
      }

      @Override
      protected void failed(Exception e) {
        fail(e);
      }
    };

    MessageDispatcher serverDispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
          case Const.TO0_HELLO:
          case Const.TO0_OWNER_SIGN:
            return createTo0Service(cs, ds);
          case Const.TO1_HELLO_RV:
          case Const.TO1_PROVE_TO_RV:
            return createTo1Service(cs, ds, ods);
          default:
            throw new DispatchException(new IllegalArgumentException());
        }
      }

      @Override
      protected void failed(Exception e) {
        fail(e);
      }
    };

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    try {
      server = Server.createTcpServer(args).start();

      RvsDbManager dbManager = new RvsDbManager();
      dbManager.createTables(ds);

      DispatchResult dr = to0ClientService.getHelloMessage();

      while (!dr.isDone()) {
        dr = serverDispatcher.dispatch(dr.getReply());
        dr = clientDispatcher.dispatch(dr.getReply());
      }

      dr = to1ClientService.getHelloMessage();

      while (!dr.isDone()) {
        dr = serverDispatcher.dispatch(dr.getReply());
        dr = clientDispatcher.dispatch(dr.getReply());
      }

    } finally {
      if (server != null) {
        server.stop();
      }
    }

  }
}
