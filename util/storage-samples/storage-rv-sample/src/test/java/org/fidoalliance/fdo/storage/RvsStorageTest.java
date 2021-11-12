// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.DispatchException;
import org.fidoalliance.fdo.protocol.DispatchResult;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.RendezvousBlobDecoder;
import org.fidoalliance.fdo.protocol.To0ClientService;
import org.fidoalliance.fdo.protocol.To0ClientStorage;
import org.fidoalliance.fdo.protocol.To0ServerService;
import org.fidoalliance.fdo.protocol.To0ServerStorage;
import org.fidoalliance.fdo.protocol.To1ClientService;
import org.fidoalliance.fdo.protocol.To1ClientStorage;
import org.fidoalliance.fdo.protocol.To1ServerService;
import org.fidoalliance.fdo.protocol.To1ServerStorage;
import org.fidoalliance.fdo.protocol.ondie.OnDieCache;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;
import org.h2.tools.Server;
import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(Alphanumeric.class)
public class RvsStorageTest {

  private static final LoggerService logger = new LoggerService(RvsStorageTest.class);

  private static final String DB_HOST = "localhost";
  private static final String DB_PORT = "8091";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private static final String RV_BLOB = "http://localhost:8090?ipaddress=127.0.0.1";

  private static final String VOUCHER = ""
      + "848618645086a8f055ac1d4a5cb491e250f4fec1b081858205696c6f63616c686f73748203191f68820c01820"
      + "2447f0000018204191f696a44656d6f446576696365832601585b3059301306072a8648ce3d020106082a8648"
      + "ce3d030107034200042c02709032b3fc1696ab55b1ecf8e44795b92cb21b6a681265e54d525c8533fb74b0c03"
      + "10166ef11b0f32aef76e135f86acdd65633267de932b31df43e50c625822f5820908af13922c6c8b3a35fd335"
      + "953964296fbefc2ebaa9bcf12eb107f99ec545d382055820965d7c2c22407555d1a1387c748fa17ede6d1244d"
      + "f799931f7f3fbd03d66e3d2825901013081fe3081a5a0030201020206017d1506c30c300a06082a8648ce3d04"
      + "0302300d310b300906035504030c024341301e170d3231313131323136343134345a170d33313131313031363"
      + "43134345a30003059301306072a8648ce3d020106082a8648ce3d03010703420004a582f072ec6a4746d8e7c9"
      + "74558a6c4ec694ce91420a978dddb995d201e9e712c7330bc1151c8eb656313745dac7c7040ec7ef22e549621"
      + "632b5b3863e467c98300a06082a8648ce3d0403020348003045022100dacd5a0f10388cb7e39a1eeeeef66d32"
      + "9a7d696e18c0c87f54ad13bb926026020220206235dfae094eabb2d9195127a1f931cc6269d8057f3c9c44a59"
      + "a703713d3ec590126308201223081c9a003020102020900a4d303ae980f53f1300a06082a8648ce3d04030230"
      + "0d310b300906035504030c0243413020170d3139303432343134343634375a180f32303534303431353134343"
      + "634375a300d310b300906035504030c0243413059301306072a8648ce3d020106082a8648ce3d030107034200"
      + "042c02709032b3fc1696ab55b1ecf8e44795b92cb21b6a681265e54d525c8533fb74b0c0310166ef11b0f32ae"
      + "f76e135f86acdd65633267de932b31df43e50c625a310300e300c0603551d13040530030101ff300a06082a86"
      + "48ce3d0403020348003045022100a5419b823613d24eb701e440b4f3368be5675ba72461a272bc52eeb96c3e4"
      + "14002204e70d27b631cb6efc26aa0c027e1e53eaef1ec5074203683d1ecbb9de129c69281d28443a10126a058"
      + "a983822f582004d74e32a999e43c32d5d93071a87112774108dd9f08bd12b3d3dec93394f965822f582071869"
      + "491f3e3e1e02cac8147f6b72ce7809787b9f7bec2a1cda6580ed9b7d8c2832601585b3059301306072a8648ce"
      + "3d020106082a8648ce3d03010703420004595504d86d062f2f2c72600ec90ca1701885fdf4947778bf3a0ed70"
      + "d286225bd88b1b099491aadd5e935e486de088e73ec11de6b61991a068aeb77320f5e6034584093591cbfe770"
      + "255511a0b2556aa006aa25a1891dc3eb512e452bb57fe6579dcaf1c7b143c946fcb2de5c39b0896cbf2488e8f"
      + "436cc5ec4d594a0f812073b3dec";

  protected static String deviceCreds = ""
      + "87f51864582054686973206973206120534841323536206b657920666f7220686d616320616c6a44656d6f446"
      + "5766963655086a8f055ac1d4a5cb491e250f4fec1b081858205696c6f63616c686f73748203191f68820c0182"
      + "02447f0000018204191f69822f582032f840822e21d4b8a5ee1bf66bb3a08ead5f5f7e4a086576945447e46cb"
      + "fae8f";

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

      final String configFilePath = "src/test/resources/config.properties";
      RvsDbManager rvsDbManager = new RvsDbManager();
      assertDoesNotThrow( ()-> {
            rvsDbManager.createAllowListDenyListTables(ds);
            rvsDbManager.importGuidFromDenyList(ds, configFilePath);
            rvsDbManager.importAllowListKeyHash(ds, configFilePath);
            rvsDbManager.importDenyListKeyHash(ds, configFilePath);
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
        logger.info("To0 Wait: " + wait + " seconds");
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
        logger.info("To1d" + signedBlob.toString());
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
