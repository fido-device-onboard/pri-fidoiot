// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.web;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import org.fido.iot.certutils.PemLoader;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.DispatchResult;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.RendezvousBlobDecoder;
import org.fido.iot.protocol.RendezvousInfoDecoder;
import org.fido.iot.protocol.To0ClientService;
import org.fido.iot.protocol.To0ClientStorage;

public class To0ClientApp {

  private static final int REQUEST_WS = 3600;
  private static final String RV_BLOB = "http://localhost:8042?ipaddress=127.0.0.1";

  private static final String VOUCHER = ""
      + "8486186450f0956089c0df4c349c61f460457e87eb81858205696c6f63616c686f73748203191f68820c01820"
      + "2447f0000018204191f686a44656d6f446576696365830d0258402c02709032b3fc1696ab55b1ecf8e44795b9"
      + "2cb21b6a681265e54d525c8533fb74b0c0310166ef11b0f32aef76e135f86acdd65633267de932b31df43e50c"
      + "62582085820744c9e7af744e7408e288d27017d0904605eed5bc07e7c1771404e569bdfe3e6820558205cd7dd"
      + "9fd13c8f681dc42208f4770ccbe10b6beb178cb595bacd53bc3f61e3b18259017a308201763082011d0209008"
      + "da355b7e71c51f5300a06082a8648ce3d040302300d310b300906035504030c0243413020170d313931313232"
      + "3135353430315a180f32303534313131333135353430315a3078310b3009060355040613025553310f300d060"
      + "35504080c064f7265676f6e3112301006035504070c0948696c6c73626f726f310e300c060355040a0c05496e"
      + "74656c311d301b060355040b0c14446576696365204d616e75666163747572696e673115301306035504030c0"
      + "c44656d6f44657669636532373059301306072a8648ce3d020106082a8648ce3d03010703420004a582f072ec"
      + "6a4746d8e7c974558a6c4ec694ce91420a978dddb995d201e9e712c7330bc1151c8eb656313745dac7c7040ec"
      + "7ef22e549621632b5b3863e467c98300a06082a8648ce3d040302034700304402204386077f39aee794f7e48e"
      + "af04ff4c18822a8c306994ad4ad75ccab5aef7478c022073ce183429452662c29d4c4d1b750f63167e85c9cb0"
      + "ef7b2581a986ec9282bf1590126308201223081c9a003020102020900a4d303ae980f53f1300a06082a8648ce"
      + "3d040302300d310b300906035504030c0243413020170d3139303432343134343634375a180f3230353430343"
      + "1353134343634375a300d310b300906035504030c0243413059301306072a8648ce3d020106082a8648ce3d03"
      + "0107034200042c02709032b3fc1696ab55b1ecf8e44795b92cb21b6a681265e54d525c8533fb74b0c0310166e"
      + "f11b0f32aef76e135f86acdd65633267de932b31df43e50c625a310300e300c0603551d13040530030101ff30"
      + "0a06082a8648ce3d0403020348003045022100a5419b823613d24eb701e440b4f3368be5675ba72461a272bc5"
      + "2eeb96c3e414002204e70d27b631cb6efc26aa0c027e1e53eaef1ec5074203683d1ecbb9de129c6928184a101"
      + "2640588e838208582099713d28d33bb5fa29f77f0da8ff182e5a076670a4ec23244ed504ec6f10fd0f8208582"
      + "082d4659e9dbbc7fac58ad015faf42ac0947ee511d752ab37edc42eb0d969df28830d025840595504d86d062f"
      + "2f2c72600ec90ca1701885fdf4947778bf3a0ed70d286225bd88b1b099491aadd5e935e486de088e73ec11de6"
      + "b61991a068aeb77320f5e603458473045022022acd405ca7c95e8104093becea5d5ddfb25adb55012a1cc7169"
      + "ccd114977ff50221009e9cdd0815358d35d543bae8362f02ddced995ab1ff96115d423c76313ccea2c";

  private static String ownerKeyPem = "-----BEGIN CERTIFICATE-----\n"
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

  private CryptoService cryptoService;
  private Long responseWait = null;

  To0ClientStorage clientStorage = new To0ClientStorage() {

    private String clientToken;

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
      return REQUEST_WS;
    }

    @Override
    public void setResponseWait(long wait) {
      responseWait = wait;
      System.out.println("To0 Response Wait: " + Long.toString(wait));
    }

    @Override
    public PrivateKey getOwnerSigningKey(PublicKey ownerPublicKey) {
      return PemLoader.loadPrivateKey(ownerKeyPem);
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

  To0ClientService clientService = new To0ClientService() {

    @Override
    protected To0ClientStorage getStorage() {
      return clientStorage;
    }

    @Override
    public CryptoService getCryptoService() {
      return cryptoService;
    }
  };

  private MessageDispatcher createDispatcher() {

    return new MessageDispatcher() {

      @Override
      protected MessagingService getMessagingService(Composite request) {
        return clientService;
      }

      @Override
      protected void failed(Exception e) {
        e.printStackTrace();
      }
    };
  }

  private void run(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {

    cryptoService = new CryptoService();

    MessageDispatcher dispatcher = createDispatcher();

    DispatchResult dr = clientService.getHelloMessage();

    Composite ovh = clientStorage.getVoucher().getAsComposite(Const.OV_HEADER);
    Composite rvi = ovh.getAsComposite(Const.OVH_RENDEZVOUS_INFO);

    List<String> paths = RendezvousInfoDecoder.getHttpDirectives(
        rvi,
        Const.RV_OWNER_ONLY);

    for (String path : paths) {

      responseWait = null;
      try {
        WebClient client = new WebClient(path, dr, dispatcher);
        client.run();
        if (responseWait != null) {
          break;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

  /**
   * Application main.
   *
   * @param args The application arguments.
   */
  public static void main(String[] args)
      throws NoSuchAlgorithmException, IOException, InterruptedException {
    new To0ClientApp().run(args);
    System.out.println("TO0 Client finished.");
    return;
  }
}
