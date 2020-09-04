// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;
import org.fido.iot.certutils.PemLoader;

public class BaseTemplate {

  protected static final UUID guid = UUID.fromString("f0956089-c0df-4c34-9c61-f460457e87eb");

  protected static final byte[] devSecret =
      "This is a SHA256 key for hmac al".getBytes(StandardCharsets.US_ASCII);

  protected static final String RV_INFO =
      "http://localhost:8040?ipaddress=127.0.0.1&ownerport=8040";

  protected static final String RV_BLOB = "http://localhost:8042?ipaddress=127.0.0.1";

  protected static final String mfgKeyPem = "-----BEGIN CERTIFICATE-----\n"
      + "MIIBIjCByaADAgECAgkApNMDrpgPU/EwCgYIKoZIzj0EAwIwDTELMAkGA1UEAwwC\n"
      + "Q0EwIBcNMTkwNDI0MTQ0NjQ3WhgPMjA1NDA0MTUxNDQ2NDdaMA0xCzAJBgNVBAMM\n"
      + "AkNBMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELAJwkDKz/BaWq1Wx7PjkR5W5\n"
      + "LLIbamgSZeVNUlyFM/t0sMAxAWbvEbDzKu924TX4as3WVjMmfekysx30PlDGJaMQ\n"
      + "MA4wDAYDVR0TBAUwAwEB/zAKBggqhkjOPQQDAgNIADBFAiEApUGbgjYT0k63AeRA\n"
      + "tPM2i+VnW6ckYaJyvFLuuWw+QUACIE5w0ntjHLbvwmqgwCfh5T6u8exQdCA2g9Hs\n"
      + "u53hKcaS\n"
      + "-----END CERTIFICATE-----\n"
      + "-----BEGIN EC PARAMETERS-----\n"
      + "BggqhkjOPQMBBw==\n"
      + "-----END EC PARAMETERS-----\n"
      + "-----BEGIN EC PRIVATE KEY-----\n"
      + "MHcCAQEEIJTKW2/54N85RLJu0C5fEkAwQiKqxRqHzx5PUfd/M66UoAoGCCqGSM49\n"
      + "AwEHoUQDQgAELAJwkDKz/BaWq1Wx7PjkR5W5LLIbamgSZeVNUlyFM/t0sMAxAWbv\n"
      + "EbDzKu924TX4as3WVjMmfekysx30PlDGJQ==\n"
      + "-----END EC PRIVATE KEY-----";

  protected static final String devKeyPem = "-----BEGIN CERTIFICATE-----\n"
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

  protected static final char[] STORE_CRED = "".toCharArray();

  protected CryptoService cryptoService;

  protected MessageDispatcher serverDispatcher;
  protected MessagingService serverService;

  protected MessageDispatcher clientDispatcher;
  protected ClientService clientService;

  //etSocketAddress sdoAddress;
  protected Composite singedRedirect;
  protected Composite unsignedRedirect;

  protected Composite voucher;
  protected Composite deviceCreds;

  protected Composite createTestVoucher() {
    Composite voucher = Composite.newArray();
    Composite header = Composite.newArray();

    //setup cert hash and public key
    Composite chain = Composite.newArray();
    chain.set(chain.size(), PemLoader.loadCerts(devKeyPem).get(0));
    chain.set(chain.size(), PemLoader.loadCerts(mfgKeyPem).get(0));

    cryptoService.verify(chain);

    Composite pub = cryptoService.encode(
        PemLoader.loadCerts(mfgKeyPem).get(0).getPublicKey(),
        Const.PK_ENC_COSEEC);

    Composite hash = cryptoService.hash(Const.SHA_256, chain.toBytes());

    //build info rendezvous
    Composite rv = RendezvousInfoDecoder.decode(RV_INFO);

    header.set(Const.OVH_VERSION, Const.PROTOCOL_VERSION_100);
    header.set(Const.OVH_GUID, guid);
    header.set(Const.OVH_RENDEZVOUS_INFO, rv);
    header.set(Const.OVH_DEVICE_INFO, "DemoDevice");
    header.set(Const.OVH_PUB_KEY, pub);
    //todo:handle null hash
    header.set(Const.OVH_CERT_CHAIN_HASH, hash);

    voucher.set(Const.OV_HEADER, header);
    voucher.set(Const.OV_HMAC, Composite.newArray());
    //todo: handle null chain
    voucher.set(Const.OV_DEV_CERT_CHAIN, chain);
    voucher.set(Const.OV_ENTRIES, Composite.newArray());

    return voucher;
  }

  protected Composite getVoucherHmac(CryptoService service, Composite ovh) {
    return service.hash(Const.HMAC_SHA_256, devSecret, ovh.toBytes());
  }

  protected void printError(Composite msg) {
    Composite error = msg.getAsComposite(Const.SM_BODY);
    System.out.println(error.getAsString(Const.EM_ERROR_STR));
  }

  protected void setup() throws Exception {

    //setup resources used for tests
    SecureRandom random = new SecureRandom();

    //setup crypto service
    cryptoService = new CryptoService();

    voucher = createTestVoucher();

    //update HMAC
    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    voucher.set(Const.OV_HMAC, getVoucherHmac(cryptoService, ovh));

    //extend the voucher
    VoucherExtensionService oes = new VoucherExtensionService(voucher, cryptoService);
    oes.add(PemLoader.loadPrivateKey(mfgKeyPem),
        PemLoader.loadCerts(ownerKeyPem).get(0).getPublicKey());

    //setup a message dispatcher
    serverDispatcher = new MessageDispatcher() {

      @Override
      public MessagingService getMessagingService(Composite msg) {
        return serverService;
      }

      @Override
      protected void dispatching(Composite request) {
        System.out.println("dispatching: " + request.toString());
        ;
      }

      @Override
      protected void failed(Exception e) {
        fail(e);
      }

    };

    clientDispatcher = new MessageDispatcher() {

      @Override
      public MessagingService getMessagingService(Composite msg) {
        return clientService;
      }

      @Override
      protected void dispatching(Composite request) {
        System.out.println("dispatching: " + request.toString());
      }

      @Override
      protected void failed(Exception e) {
        fail(e);
      }
    };

    //create device credential from voucher
    deviceCreds = Composite.newArray()
        .set(Const.DC_ACTIVE, true)
        .set(Const.DC_PROTVER, voucher.getAsComposite(Const.OV_HEADER)
            .getAsNumber(Const.OVH_VERSION).intValue())
        .set(Const.DC_HMAC_SECRET, devSecret)
        .set(Const.DC_DEVICE_INFO, "DemoDevice")
        .set(Const.DC_GUID,
            voucher.getAsComposite(Const.OV_HEADER)
                .getAsBytes(Const.OVH_GUID))
        .set(Const.DC_RENDEZVOUS_INFO,
            voucher.getAsComposite(Const.OV_HEADER)
                .getAsComposite(Const.OVH_RENDEZVOUS_INFO));

    //create a redirect blob
    unsignedRedirect = RendezvousBlobDecoder.decode(RV_BLOB);

    singedRedirect = Composite.newArray()

        .set(Const.TO1D_RV, unsignedRedirect)
        .set(Const.TO1D_TO0D_HASH, Const.EMPTY_MESSAGE);

  }

  protected void runClient(DispatchResult dr) throws Exception {

    MessageDispatcher dispatcher = clientDispatcher;
    while (!dr.isDone()) {
      dr = serverDispatcher.dispatch(dr.getReply());
      dr = clientDispatcher.dispatch(dr.getReply());
    }

    System.out.println("Client protocol finished.");
  }
}
