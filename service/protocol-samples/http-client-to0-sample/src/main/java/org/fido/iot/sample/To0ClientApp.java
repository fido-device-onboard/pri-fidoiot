// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
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

  private static final String sampleVoucher = ""
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

  private static String sampleOwnerKeyPemEC256 = "-----BEGIN CERTIFICATE-----\n"
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

  private static String sampleOwnerKeyPemEC384 = "-----BEGIN PUBLIC KEY-----\n"
      + "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEMNMHB3t2Po763C8QteK7/STJRf6F1Sfk\n"
      + "yi2TYmGWdnlXgI+5s7fOkrJzebHGvg61vfpSZ3qcrKJqU6EkWQvy+fqHH609U00W\n"
      + "hNwLYKjiGqtVlBrBs0Q9vPBZVBPiN3Ji\n"
      + "-----END PUBLIC KEY-----\n"
      + "-----BEGIN PRIVATE KEY-----\n"
      + "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDA1SeyFqosrLr1hCgzs\n"
      + "B0G3Hi5y1YhWKo/Rz3dVeRnKPwysEMIGIcdt2meTkr5dJs2hZANiAAQw0wcHe3Y+\n"
      + "jvrcLxC14rv9JMlF/oXVJ+TKLZNiYZZ2eVeAj7mzt86SsnN5sca+DrW9+lJnepys\n"
      + "ompToSRZC/L5+ocfrT1TTRaE3AtgqOIaq1WUGsGzRD288FlUE+I3cmI=\n"
      + "-----END PRIVATE KEY-----\n";

  private static String sampleOwnerKeyPemRSA = "-----BEGIN CERTIFICATE-----\n"
      + "MIIDazCCAlOgAwIBAgIUIK/VIIEW6iqeu34vJ2pi0gGVGiEwDQYJKoZIhvcNAQEL\n"
      + "BQAwRTELMAkGA1UEBhMCVVMxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM\n"
      + "GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMDExMTEyMzUwMDBaFw0yMTEx\n"
      + "MDYyMzUwMDBaMEUxCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw\n"
      + "HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB\n"
      + "AQUAA4IBDwAwggEKAoIBAQDBNaM7ZZORAlJG5/WwiXHfrlXE7F0gCcNm78Zg0yDy\n"
      + "goNqmDS6+L8OC4WSvFuzcC1EcZZ041hdsZ3VmwZxVj0OwINHlHn40LHwQRh9hJvP\n"
      + "zx2+dwkwSruImJBc11qP10Ie5mpqPksYKEFwsuV3KdebHdkMTvzRoo8aiaN5cSpx\n"
      + "aCl5vQvg768bGS/PjYLVe2+WyME08YoCMwzrFE+SistJ8LU/RfIRHeSpVhzj9wz0\n"
      + "XI7v+8cvAf1vI8JXhZtSmS2L9sS0+z3EucS1g/ShXfQ9x0VU6DyFy5oTLoaix5TU\n"
      + "SGWmBYl4GxscxOU4HbQklrSR8XNERokQ3IfqcdbdtJQDAgMBAAGjUzBRMB0GA1Ud\n"
      + "DgQWBBTifM5fBBwAGWkNYRDYtfHmcmxllTAfBgNVHSMEGDAWgBTifM5fBBwAGWkN\n"
      + "YRDYtfHmcmxllTAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAF\n"
      + "/7NLSh2bj/lEasIXNfrzHeQVToB8B//zUx41sU+4UN5DmMkXidqSuXNaR17mFu0o\n"
      + "R5icwkHGP8r4jqspMi8KkWarlBbscED3DhgBK7G8SdmaZyTF1L0egKGJR1d9gVvy\n"
      + "3GoJ/QC4pQnpqCdEX50vSEti/W1qTtvXnPl+85wo+9ZEDsnka23U+SrCVnHZ92Tg\n"
      + "Y2oZVlmVkWeymcIy5xJB9j68pvx4P+2Ki1HPyr23ykI1tu5EkhVnSkFiHhwF98w/\n"
      + "YWHqNhvEwYD0li4F62lopZJrPiEXH/AMFAeKYbMsF/1WiS641o0XQ2Apd5ElRlhg\n"
      + "UHhy+Y1ymsW9HnsSVgxW\n"
      + "-----END CERTIFICATE-----\n"
      + "-----BEGIN RSA PRIVATE KEY-----\n"
      + "MIIEowIBAAKCAQEAwTWjO2WTkQJSRuf1sIlx365VxOxdIAnDZu/GYNMg8oKDapg0\n"
      + "uvi/DguFkrxbs3AtRHGWdONYXbGd1ZsGcVY9DsCDR5R5+NCx8EEYfYSbz88dvncJ\n"
      + "MEq7iJiQXNdaj9dCHuZqaj5LGChBcLLldynXmx3ZDE780aKPGomjeXEqcWgpeb0L\n"
      + "4O+vGxkvz42C1XtvlsjBNPGKAjMM6xRPkorLSfC1P0XyER3kqVYc4/cM9FyO7/vH\n"
      + "LwH9byPCV4WbUpkti/bEtPs9xLnEtYP0oV30PcdFVOg8hcuaEy6GoseU1EhlpgWJ\n"
      + "eBsbHMTlOB20JJa0kfFzREaJENyH6nHW3bSUAwIDAQABAoIBAGf8/3rzg8XzUuc4\n"
      + "52J5x2FVinIvqTuaJaJcgRAR8lSz7BlODGtpgCMGDoFYgZ6FTMfevtTwm9npxAJ4\n"
      + "qrILcVdkRAQdpLxHTs35qz27RsVFO0QM+1QTlPXC70gY3HQ4rizFZRcDqhU5bWdD\n"
      + "0f8d9R5WpUBbrvUBojXtSKAPSYG5Vx0t+Edx6I21Je7fRp3x3QIFAAGtaVDFP6ZE\n"
      + "t3Q/WJSr+PGmvLJBcxa+KB8h90UH8mmNIQ0ns6E1MCGHgPlGre7k98ZfmJtrdF02\n"
      + "7R/6yjOYBOuBZGj9NhVrNTFlbkyArX5opmfCYmF6gxpVEXvGqwfxkaYStYtgCAyw\n"
      + "StX/NvECgYEA8JY1U3xNYqTaBjDMcMP638Hjr4qxVP7EuNfB6LT92OPDk+k7dah6\n"
      + "c/a6ZPy2SQoYRd0KvddnMAXUTKCkBk6NQzrYwBgCOlP7ZFS49V8o891AB4GVxpAs\n"
      + "YK5kOBmr28DrDSoqgrq2VtXWPsophWj6jOo7kiRMVd/HeG4crWXFFnsCgYEAzZZo\n"
      + "1Aq1w4bgUIr7le51ahXWIBENgUhyhw6QwSiPYv6czLMm6Wltr/f9aF1G1G0kkPFx\n"
      + "haKV5dOsRpl/XD2YXURb5jzBSCnI8B5nps2BFRZylRo8DpQPTc/u5x3Xsu+mV+cO\n"
      + "wqjr1pqMdsxrIx8nuK5J8F5eddTCrO+LuKzDhhkCgYBhWQYez88oOPgXRSuT/VjH\n"
      + "rVaJsx7p+2RyPqsxk/qbBh4e/WKXyyIxRgZJ0o+XokQMENLF2iDgp5stnlqASsyS\n"
      + "BX+qyowsOcsg378vWd/iPQYpg1+tRq0OOcJHiOunMbpqS0OEPjSFkmTsZzTBzKaE\n"
      + "2kpcad+RZ8j4i+WCrAlBfQKBgEOTmyWH17NLlFQlOwlPdpzGTLoBoPTMAYlPSlmT\n"
      + "oX4ivxuyD6pNk1ZyJ4O0lWGh9pEGLBCHHsqKx2f1cJ27SWfu3l5Rvh6gTYJQHDYB\n"
      + "+toZpNRJ6U/JbzZekOK7NnmEuKLQOlPV/p9t8ZnjG5xR01arQ3aVLmuA4fTCUKUF\n"
      + "dMeRAoGBAKURcxU9QPUiHTwszIJnn2WpvBjZ/KmjvdiMAKWp5uL1pAuag5cX3Wvu\n"
      + "5nhvQjoOBW30KgCxvwvNz8AWYboKIqWgm/pQNPDVb78O75pSyKkXHF7Rn0qR3KOw\n"
      + "RvhMko1PjCZxnDBtOmriskFMnTrlMBgu/jAbebJFbDOPSpcFMNcS\n"
      + "-----END RSA PRIVATE KEY-----\n"
      + "-----BEGIN PUBLIC KEY-----\n"
      + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwTWjO2WTkQJSRuf1sIlx\n"
      + "365VxOxdIAnDZu/GYNMg8oKDapg0uvi/DguFkrxbs3AtRHGWdONYXbGd1ZsGcVY9\n"
      + "DsCDR5R5+NCx8EEYfYSbz88dvncJMEq7iJiQXNdaj9dCHuZqaj5LGChBcLLldynX\n"
      + "mx3ZDE780aKPGomjeXEqcWgpeb0L4O+vGxkvz42C1XtvlsjBNPGKAjMM6xRPkorL\n"
      + "SfC1P0XyER3kqVYc4/cM9FyO7/vHLwH9byPCV4WbUpkti/bEtPs9xLnEtYP0oV30\n"
      + "PcdFVOg8hcuaEy6GoseU1EhlpgWJeBsbHMTlOB20JJa0kfFzREaJENyH6nHW3bSU\n"
      + "AwIDAQAB\n"
      + "-----END PUBLIC KEY-----\n";

  protected static String[] sampleOwnerPemKeys =
      {sampleOwnerKeyPemEC256, sampleOwnerKeyPemEC384, sampleOwnerKeyPemRSA};

  private CryptoService cryptoService;
  private Long responseWait = null;

  To0ClientStorage clientStorage = new To0ClientStorage() {

    private String clientToken;

    @Override
    public Composite getVoucher() {
      // if the optional "voucher" file is present then use
      // it. If not, then fall back to the hardcoded value.

      try {
        FileInputStream fis = new FileInputStream("voucher");
        byte[] voucherBytes = fis.readAllBytes();
        return Composite.fromObject(voucherBytes);
      } catch (Exception ex) {
        return Composite.fromObject(sampleVoucher);
      }
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
      String pemValue = sampleOwnerKeyPemEC256;
      if (ownerPublicKey instanceof ECKey) {
        int bitLength = ((ECKey) ownerPublicKey).getParams().getCurve().getField().getFieldSize();
        if (bitLength == Const.BIT_LEN_256) {
          pemValue = sampleOwnerKeyPemEC256;
        } else if (bitLength == Const.BIT_LEN_384) {
          pemValue = sampleOwnerKeyPemEC384;
        }
      } else if (ownerPublicKey instanceof RSAKey) {
        pemValue = sampleOwnerKeyPemRSA;
      }
      return PemLoader.loadPrivateKey(pemValue);
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
        System.out.println(e.getMessage());
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
        System.out.println(e.getMessage());
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
