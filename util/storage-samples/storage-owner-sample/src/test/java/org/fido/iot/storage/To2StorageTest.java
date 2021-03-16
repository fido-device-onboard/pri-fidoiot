// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.fido.iot.sample.DeviceServiceInfoModule;
import org.fido.iot.sample.DeviceServiceInfoSequence;
import org.fido.iot.serviceinfo.ServiceInfo;
import org.fido.iot.serviceinfo.ServiceInfoEntry;
import org.fido.iot.serviceinfo.ServiceInfoMarshaller;
import org.h2.tools.Server;
import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
import org.junit.jupiter.api.Test;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.DispatchResult;
import org.fido.iot.protocol.KeyResolver;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.ServiceInfoEncoder;
import org.fido.iot.protocol.To2ClientService;
import org.fido.iot.protocol.To2ClientStorage;
import org.fido.iot.protocol.To2ServerService;
import org.fido.iot.protocol.To2ServerStorage;
import org.junit.jupiter.api.TestMethodOrder;


@TestMethodOrder(Alphanumeric.class)
public class To2StorageTest {

  private static final String DB_HOST = "localhost";
  private static final String DB_PORT = "8043";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";

  private static final String VOUCHER = ""
      + "8486186450f0956089c0df4c349c61f460457e87eb8185820567302e302e302e3082024400000000820419cb9"
      + "f820319cb9f820b146a44656d6f446576696365830d0258402c02709032b3fc1696ab55b1ecf8e44795b92cb2"
      + "1b6a681265e54d525c8533fb74b0c0310166ef11b0f32aef76e135f86acdd65633267de932b31df43e50c6258"
      + "2085820744c9e7af744e7408e288d27017d0904605eed5bc07e7c1771404e569bdfe3e682055820cf2b488f87"
      + "d0af1755d7aeb775879a14bc3d0c5989af0db0dfdf235ed06bfe538259017a308201763082011d0209008da35"
      + "5b7e71c51f5300a06082a8648ce3d040302300d310b300906035504030c0243413020170d3139313132323135"
      + "353430315a180f32303534313131333135353430315a3078310b3009060355040613025553310f300d0603550"
      + "4080c064f7265676f6e3112301006035504070c0948696c6c73626f726f310e300c060355040a0c05496e7465"
      + "6c311d301b060355040b0c14446576696365204d616e75666163747572696e673115301306035504030c0c446"
      + "56d6f44657669636532373059301306072a8648ce3d020106082a8648ce3d03010703420004a582f072ec6a47"
      + "46d8e7c974558a6c4ec694ce91420a978dddb995d201e9e712c7330bc1151c8eb656313745dac7c7040ec7ef2"
      + "2e549621632b5b3863e467c98300a06082a8648ce3d040302034700304402204386077f39aee794f7e48eaf04"
      + "ff4c18822a8c306994ad4ad75ccab5aef7478c022073ce183429452662c29d4c4d1b750f63167e85c9cb0ef7b"
      + "2581a986ec9282bf1590126308201223081c9a003020102020900a4d303ae980f53f1300a06082a8648ce3d04"
      + "0302300d310b300906035504030c0243413020170d3139303432343134343634375a180f32303534303431353"
      + "134343634375a300d310b300906035504030c0243413059301306072a8648ce3d020106082a8648ce3d030107"
      + "034200042c02709032b3fc1696ab55b1ecf8e44795b92cb21b6a681265e54d525c8533fb74b0c0310166ef11b"
      + "0f32aef76e135f86acdd65633267de932b31df43e50c625a310300e300c0603551d13040530030101ff300a06"
      + "082a8648ce3d0403020348003045022100a5419b823613d24eb701e440b4f3368be5675ba72461a272bc52eeb"
      + "96c3e414002204e70d27b631cb6efc26aa0c027e1e53eaef1ec5074203683d1ecbb9de129c692818443a1012680"
      + "588e8382085820b7db8ebbceb119147d28a70ae50de328cdb7d7984ecf147b90d117ac721a6c128208582082d"
      + "4659e9dbbc7fac58ad015faf42ac0947ee511d752ab37edc42eb0d969df28830d025840595504d86d062f2f2c"
      + "72600ec90ca1701885fdf4947778bf3a0ed70d286225bd88b1b099491aadd5e935e486de088e73ec11de6b619"
      + "91a068aeb77320f5e6034584830460221009eb8a5a1d81e3bb69c0f3a6844e280d2af67119ac5e53109a45129"
      + "be247726510221008085c9b2029b5171dd1780a038ce5059fece59d36fb086db6e25adcdedaa9c0c";

  protected static String deviceCreds = ""
      + "87f51864582054686973206973206120534841323536206b657920666f7220686d616320616c6a44656d6f44657"
      + "669636550f0956089c0df4c349c61f460457e87eb81858205696c6f63616c686f73748203191f68820c01820244"
      + "7f0000018204191f68820858205603b28472872ecbb5d4981fbaa91664ec8627ea395d2bbee7a85e0f99a7ed34";

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

  private final String ownerKeysPem = "-----BEGIN PUBLIC KEY-----\n"
      + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSU\n"
      + "d3i/Og7XDShiJb2IsbCZSRqt1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
      + "-----END PUBLIC KEY-----\n"
      + "-----BEGIN PUBLIC KEY-----\n"
      + "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE4RFfGVQdojLIODXnUT6NqB6KpmmPV2Rl\n"
      + "aVWXzdDef83f/JT+/XLPcpAZVoS++pwZpDoCkRU+E2FqKFdKDDD4g7obfqWd87z1\n"
      + "EtjdVaI1qiagqaSlkul2oQPBAujpIaHZ\n"
      + "-----END PUBLIC KEY-----\n"
      + "-----BEGIN PUBLIC KEY-----\n"
      + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwTWjO2WTkQJSRuf1sIlx\n"
      + "365VxOxdIAnDZu/GYNMg8oKDapg0uvi/DguFkrxbs3AtRHGWdONYXbGd1ZsGcVY9\n"
      + "DsCDR5R5+NCx8EEYfYSbz88dvncJMEq7iJiQXNdaj9dCHuZqaj5LGChBcLLldynX\n"
      + "mx3ZDE780aKPGomjeXEqcWgpeb0L4O+vGxkvz42C1XtvlsjBNPGKAjMM6xRPkorL\n"
      + "SfC1P0XyER3kqVYc4/cM9FyO7/vHLwH9byPCV4WbUpkti/bEtPs9xLnEtYP0oV30\n"
      + "PcdFVOg8hcuaEy6GoseU1EhlpgWJeBsbHMTlOB20JJa0kfFzREaJENyH6nHW3bSU\n"
      + "AwIDAQAB\n"
      + "-----END PUBLIC KEY-----";

  private static final String BASE_PATH = Path.of(
      System.getProperty("user.dir"),
      "target", "data",
      "ops").toString();

  private static final String packageContent =
      "#!/bin/bash\r\n"
      + "filename=payload.bin\r\n"
      + "cksum_tx=1612472339\r\n"
      + "cksum_rx=$(cksum $filename | cut -d ' ' -f 1)\r\n"
      + "if [ $cksum_tx -eq $cksum_rx  ]; then\r\n"
      + "  echo \"Device onboarded successfully.\"\r\n"
      + "  echo \"Device onboarded successfully.\" > result.txt\r\n"
      + "else\r\n"
      + "  echo \"ServiceInfo file transmission failed.\"\r\n"
      + "  echo \"ServiceInfo file transmission failed.\" > result.txt\r\n"
      + "fi\r\n";

   private static final String fileContent = "sample file";

  String activateMod = "true";
  String packageName = "linux64.sh";
  String filename = "sample_file";
  String url = "http://host/file.tmp";
  String sviString = "sdo_sys:filedesc=packageName,sdo_sys:write=packageContent" +
  ",sdo_wget:filename=filename,sdo_wget:url=url";

  static BasicDataSource ds = new BasicDataSource();
  static {
    ds.setUrl("jdbc:h2:tcp://" + DB_HOST + ":" + DB_PORT + "/" + BASE_PATH);
    ds.setDriverClassName("org.h2.Driver");
    ds.setUsername(DB_USER);
    ds.setPassword(DB_PASSWORD);

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

  }

  final KeyResolver keyResolver = new KeyResolver() {
    @Override
    public PrivateKey getKey(PublicKey key) {
      return PemLoader.loadPrivateKey(ownerKeyPem);
    }
  };

  String clientToken;
  Composite toOwnerInfo;

  private To2ServerService createTo2Service(CryptoService cs, DataSource ds) {
    return new To2ServerService() {
      private To2ServerStorage storage;

      @Override
      public To2ServerStorage getStorage() {
        if (storage == null) {
          storage = new OwnerDbStorage(cs, ds, keyResolver, null);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }

  private void insertSampleServiceInfo(DataSource ds, OwnerDbManager ownerDbManager) {

    ownerDbManager.addServiceInfo(ds, "activate_mod", activateMod.getBytes());
    ownerDbManager.addServiceInfo(ds, "packageContent", packageContent.getBytes());
    ownerDbManager.addServiceInfo(ds, "sample_file", fileContent.getBytes());
    ownerDbManager.addServiceInfo(ds, "packageName", packageName.getBytes());
    ownerDbManager.addServiceInfo(ds, "filename", filename.getBytes());
    ownerDbManager.addServiceInfo(ds, "url", url.getBytes());
  }

  private void insertSampleSettings(DataSource ds, OwnerDbManager ownerDbManager) {
    ownerDbManager.loadTo2Settings(ds);
    ownerDbManager.addDeviceTypeOwnerSviString(ds, "default", sviString);
    ownerDbManager.addCustomer(ds, 1, "owner", ownerKeysPem);
  }

  @Test
  void accTest() throws Exception {

    Composite testV = Composite.fromObject(VOUCHER);
    BasicDataSource ds = new BasicDataSource();

    ds.setUrl("jdbc:h2:tcp://" + DB_HOST + ":" + DB_PORT + "/" + BASE_PATH);
    ds.setDriverClassName("org.h2.Driver");
    ds.setUsername(DB_USER);
    ds.setPassword(DB_PASSWORD);

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);
    CryptoService cs = new CryptoService();
    DeviceServiceInfoModule deviceServiceInfoModule = new DeviceServiceInfoModule();

    To2ClientStorage to2ClientStorage = new To2ClientStorage() {
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
      public String getKexSuiteName() {
        return Const.ECDH_ALG_NAME;
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
        List<Composite> list = new ArrayList<>();
        ServiceInfoMarshaller marshaller = new ServiceInfoMarshaller(getMaxDeviceServiceInfoMtuSz(),
                Composite.fromObject(VOUCHER).getAsComposite(Const.OV_HEADER)
                        .getAsUuid(Const.OVH_GUID));
        marshaller.register(new DeviceServiceInfoModule());
        Iterable<Supplier<ServiceInfo>> serviceInfos = marshaller.marshal();
        for (final Iterator<Supplier<ServiceInfo>> it = serviceInfos.iterator(); it.hasNext();) {
          ServiceInfo serviceInfo = it.next().get();
            // Convert to CBOR now
            Iterator<ServiceInfoEntry> marshalledEntries = serviceInfo.iterator();
            while (marshalledEntries.hasNext()) {
              ServiceInfoEntry marshalledEntry = marshalledEntries.next();
              Composite innerArray = ServiceInfoEncoder.encodeValue(marshalledEntry.getKey(),
                      marshalledEntry.getValue().getContent());
              list.add(innerArray);
            }
          }
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
        //Length field is zero as it will not be used by putServiceInfo.
        deviceServiceInfoModule.putServiceInfo(
                Composite.fromObject(VOUCHER).getAsComposite(Const.OV_HEADER)
                        .getAsUuid(Const.OVH_GUID),
                new ServiceInfoEntry(info.getAsString(Const.FIRST_KEY),
                        new DeviceServiceInfoSequence(info.getAsString(Const.FIRST_KEY),
                                info.getAsBytes(Const.SECOND_KEY), 0)));
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

    To2ClientService to2ClientService = new To2ClientService() {
      @Override
      protected To2ClientStorage getStorage() {
        return to2ClientStorage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };

    MessageDispatcher clientDispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return to2ClientService;
      }

      @Override
      protected void failed(Exception e) {
        fail(e);
      }
    };

    MessageDispatcher serverDispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return createTo2Service(cs, ds);
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

      OwnerDbManager dbsManager = new OwnerDbManager();
      dbsManager.createTables(ds);
      insertSampleServiceInfo(ds, dbsManager);
      insertSampleSettings(ds, dbsManager);
      dbsManager.importVoucher(ds, Composite.fromObject(VOUCHER));

      DispatchResult dr = to2ClientService.getHelloMessage();

      while (!dr.isDone()) {
        dr = serverDispatcher.dispatch(dr.getReply());
        dr = clientDispatcher.dispatch(dr.getReply());
      }

      dr = to2ClientService.getHelloMessage();

      while (!dr.isDone()) {
        dr = serverDispatcher.dispatch(dr.getReply());
        dr = clientDispatcher.dispatch(dr.getReply());
      }

    } finally {
      if (server != null) {
        server.stop();
      }
      try {
        // cleanup serviceinfo files that were created during test execution
        Files.deleteIfExists(Paths.get(System.getProperty("user.dir"), packageName));
      } catch (IOException e) {
        // ignore
      }
    }
  }

  @Test
  void removeVoucherTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    OwnerDbManager dbsManager = new OwnerDbManager();
    try {

      server = Server.createTcpServer(args).start();

      int res = dbsManager.removeVoucher(ds, UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb"));
      assertTrue(res == 1);

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      dbsManager.importVoucher(ds, Composite.fromObject(VOUCHER));
      if (server != null) {
        server.stop();
      }

    }
  }

  @Test
  void removeServiceInfoTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    try {

      server = Server.createTcpServer(args).start();

      OwnerDbManager dbsManager = new OwnerDbManager();
      assertDoesNotThrow( ()-> {
      dbsManager.removeServiceInfo(ds, "mod"); });

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }

  @Test
  void updateDeviceReplacementRvinfoTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    try {

      server = Server.createTcpServer(args).start();

      OwnerDbManager dbsManager = new OwnerDbManager();
      assertDoesNotThrow(
          ()-> { dbsManager.updateDeviceReplacementRvinfo(ds,
              UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb"),
              "http://localhost:8040?ipaddress=127.0.0.1&ownerport=8040");
          }
      );

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }

  @Test
  void updateDeviceReplacementGuidTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    OwnerDbManager dbsManager = new OwnerDbManager();
    try {

      server = Server.createTcpServer(args).start();
      assertDoesNotThrow(
          ()-> { dbsManager.updateDeviceReplacementGuid(ds,
              UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb"),
              UUID.fromString("f0956089-c0df-4c34-9c61-f460457e77eb"));
          }
      );

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      dbsManager.removeVoucher(ds, UUID.fromString("f0956089-c0df-4c34-9c61-f460457e77eb"));
      dbsManager.importVoucher(ds, Composite.fromObject(VOUCHER));
      if (server != null) {
        server.stop();
      }
    }

  }

  @Test
  void updateMtuTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;

    try {

      server = Server.createTcpServer(args).start();
      OwnerDbManager dbsManager = new OwnerDbManager();
      assertDoesNotThrow(
          ()-> { dbsManager.updateMtu(ds,"OWNER_MTU_THRESHOLD", 5000);
          }
      );

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }

  }

  @Test
  void updateWgetVerificationPreferenceTest() {
    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;

    try {

      server = Server.createTcpServer(args).start();
      OwnerDbManager dbsManager = new OwnerDbManager();
      assertDoesNotThrow(
          ()-> { dbsManager.updateWgetVerificationPreference(ds, true);
          }
      );

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }

  @Test
  void removeDeviceSviStringTest() {
    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;

    try {

      server = Server.createTcpServer(args).start();
      OwnerDbManager dbsManager = new OwnerDbManager();
      assertDoesNotThrow(
          ()-> { dbsManager.removeDeviceSviString(ds,"default_");
          }
      );

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }

  @Test
  void addDeviceTypeCriteriaTest() {
    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;

    try {

      server = Server.createTcpServer(args).start();
      OwnerDbManager dbsManager = new OwnerDbManager();
      assertDoesNotThrow(
          ()-> { dbsManager.addDeviceTypeCriteria(ds, "default",
              "devmod:os", "Linux");
          }
      );

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }

  @Test
  void updateReplacementKeyCustomerIdTest() {
    String args[] = new String[] {"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;

    try {

      server = Server.createTcpServer(args).start();
      OwnerDbManager dbsManager = new OwnerDbManager();
      dbsManager.importVoucher(ds, Composite.fromObject(VOUCHER));
      assertDoesNotThrow(
          () -> {
            dbsManager.addCustomer(
                ds,
                100,
                "Test",
                "-----BEGIN PUBLIC KEY-----\n"
                    + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSU\n"
                    + "d3i/Og7XDShiJb2IsbCZSRqt1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
                    + "-----END PUBLIC KEY-----\n");
            dbsManager.updateReplacementKeyCustomerId(
                ds, UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb"), 100);
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
  void addAndRemoveCustomerTest() {
    String args[] = new String[] {"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;

    try {

      server = Server.createTcpServer(args).start();
      OwnerDbManager dbsManager = new OwnerDbManager();
      assertDoesNotThrow(
          () -> {
            dbsManager.addCustomer(
                ds,
                100,
                "Test",
                "-----BEGIN PUBLIC KEY-----\n"
                    + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSU\n"
                    + "d3i/Og7XDShiJb2IsbCZSRqt1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
                    + "-----END PUBLIC KEY-----\n");
            dbsManager.removeCustomer(ds, String.valueOf(100));
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
  void fetchDevicesForTo0Test() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    OwnerDbManager dbsManager = new OwnerDbManager();
    try {

      server = Server.createTcpServer(args).start();

      OwnerDbTo0Util storageUtils = new OwnerDbTo0Util();
      List<UUID> uuid = storageUtils.fetchDevicesForTo0(ds);

      assertTrue(uuid.size() > 0);

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }

  }

  @Test
  void getResponseWaitTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    OwnerDbManager dbsManager = new OwnerDbManager();
    try {

      server = Server.createTcpServer(args).start();

      OwnerDbTo0Util storageUtils = new OwnerDbTo0Util();
      List<UUID> uuid = storageUtils.fetchDevicesForTo0(ds);
      long responseWait = storageUtils.getResponseWait(ds,uuid.get(0));
      assertTrue(responseWait >= 0);

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }


  @Test
  void getVoucherTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    UUID guid = UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb");
    OwnerDbTo0Storage storage = new OwnerDbTo0Storage(ds, keyResolver, guid);
    try {

      server = Server.createTcpServer(args).start();
      Composite voucher = storage.getVoucher();
      assertTrue(voucher.size() > 0);

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }

  }

  @Test
  void getRequestWaitTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    UUID guid = UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb");
    OwnerDbTo0Storage storage = new OwnerDbTo0Storage(ds, keyResolver, guid);
    try {

      server = Server.createTcpServer(args).start();
      long reqWait = storage.getRequestWait();
      assertTrue(reqWait > 0);

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }

  }

  @Test
  void to2StateTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    UUID guid = UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb");
    OwnerDbTo0Storage storage = new OwnerDbTo0Storage(ds, keyResolver, guid);
    try {

      server = Server.createTcpServer(args).start();
      Composite request = Composite.newArray();
      Composite response = Composite.newArray();

      assertDoesNotThrow(
          ()-> {
            storage.starting(request,response);
            storage.started(request,response);
            storage.continued(request,response);
            storage.completed(request,response);
            storage.failed(request,response);
          }
      );

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }

  }

  @Test
  void setResponseWaitTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    UUID guid = UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb");
    OwnerDbTo0Storage storage = new OwnerDbTo0Storage(ds, keyResolver, guid);
    try {

      server = Server.createTcpServer(args).start();
      assertDoesNotThrow(
          ()-> { storage.setResponseWait(3600);
          }
      );

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }

  @Test
  void insertWgetContentHashTest() {

    String args[] = new String[]{"-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", DB_PORT};
    // start the TCP Server
    Server server = null;
    try {

      server = Server.createTcpServer(args).start();
      OwnerDbManager dbManager = new OwnerDbManager();
      String res = dbManager.insertWgetContentHash(ds, sviString);
      String expected = "sdo_sys:filedesc=packageName,sdo_sys:write=packageContent,"
          + "sdo_wget:filename=filename,sdo_wget:url=url,sdo_wget:sha-384=sample_file_hash";
      assert(res.equals(expected));

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }
}

