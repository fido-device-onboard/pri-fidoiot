// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.To2ServerService;
import org.fidoalliance.fdo.protocol.To2ServerStorage;
import org.fidoalliance.fdo.protocol.ondie.OnDieCache;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;
import org.fidoalliance.fdo.storage.OwnerDbManager;
import org.fidoalliance.fdo.storage.OwnerDbStorage;

/**
 * TO2 Servlet Context Listener.
 */
public class To2ContextListener implements ServletContextListener {

  private static final LoggerService logger = new LoggerService(To2ContextListener.class);

  private static String ownerKeyPemEC256 = "-----BEGIN CERTIFICATE-----\n"
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

  private static String ownerKeyPemEC384 = "-----BEGIN PUBLIC KEY-----\n"
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

  private static String ownerKeyPemRsa = "-----BEGIN CERTIFICATE-----\n"
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

  private static final String sampleVoucher = ""
      + "84861864506d955aaa8fb142c18e1123465357a41c81858205696c6f63616c686f73748203191f68820c01820"
      + "2447f00000182041920fb6a44656d6f44657669636583260258402c02709032b3fc1696ab55b1ecf8e44795b9"
      + "2cb21b6a681265e54d525c8533fb74b0c0310166ef11b0f32aef76e135f86acdd65633267de932b31df43e50c"
      + "625822f582014b0124efcc8f680510cef6ca46ca606e8cd25db2efb3c411027c5d420fbf0178205582000afa5"
      + "540fd852f51988fcdcd692f969d0b98ec84159ccb55a664e9f92cdc8c0825901013081fe3081a5a0030201020"
      + "206017a2eee1388300a06082a8648ce3d040302300d310b300906035504030c024341301e170d323130363231"
      + "3134313633325a170d3331303631393134313633325a30003059301306072a8648ce3d020106082a8648ce3d0"
      + "3010703420004a582f072ec6a4746d8e7c974558a6c4ec694ce91420a978dddb995d201e9e712c7330bc1151c"
      + "8eb656313745dac7c7040ec7ef22e549621632b5b3863e467c98300a06082a8648ce3d0403020348003045022"
      + "100a38106af6c5db7e03b3ca20f51518c2abfd19aa0338becc6e818c8a8df64d43e02204d795e4ca260700329"
      + "0a075ca3a0ce8440af57cec5ee62d7658d70caada08b4c590126308201223081c9a003020102020900a4d303a"
      + "e980f53f1300a06082a8648ce3d040302300d310b300906035504030c0243413020170d313930343234313434"
      + "3634375a180f32303534303431353134343634375a300d310b300906035504030c0243413059301306072a864"
      + "8ce3d020106082a8648ce3d030107034200042c02709032b3fc1696ab55b1ecf8e44795b92cb21b6a681265e5"
      + "4d525c8533fb74b0c0310166ef11b0f32aef76e135f86acdd65633267de932b31df43e50c625a310300e300c0"
      + "603551d13040530030101ff300a06082a8648ce3d0403020348003045022100a5419b823613d24eb701e440b4"
      + "f3368be5675ba72461a272bc52eeb96c3e414002204e70d27b631cb6efc26aa0c027e1e53eaef1ec507420368"
      + "3d1ecbb9de129c692818443a10126a0588e83822f58205e7a79dec7414967ea9c4aa7d764975c49884924c7d9"
      + "cf45c5e78c5d01e675bc822f58206cca5ffeed27c1184948b7ff14e7d999bf473f602aa262ca9835df0299835"
      + "6df8326025840595504d86d062f2f2c72600ec90ca1701885fdf4947778bf3a0ed70d286225bd88b1b099491a"
      + "add5e935e486de088e73ec11de6b61991a068aeb77320f5e60345847304502202f8e35ae53757805722916667"
      + "80b78af8a3fc8755ec651b1967bf14d70cd52a8022100e5fb4d5c6815b151fea21fef96b9293cbb07514e249d"
      + "40479367408bce8cebde";

  private final String ownerPublicKeyPemEC256 = "-----BEGIN PUBLIC KEY-----\n"
      + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSU\n"
      + "d3i/Og7XDShiJb2IsbCZSRqt1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
      + "-----END PUBLIC KEY-----\n";
  private final String ownerPublicKeyPemEC384 = "-----BEGIN PUBLIC KEY-----\n"
      + "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEMNMHB3t2Po763C8QteK7/STJRf6F1Sfk\n"
      + "yi2TYmGWdnlXgI+5s7fOkrJzebHGvg61vfpSZ3qcrKJqU6EkWQvy+fqHH609U00W\n"
      + "hNwLYKjiGqtVlBrBs0Q9vPBZVBPiN3Ji\n"
      + "-----END PUBLIC KEY-----\n";

  private final String ownerPublicKeysPemRsa = "-----BEGIN PUBLIC KEY-----\n"
      + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwTWjO2WTkQJSRuf1sIlx\n"
      + "365VxOxdIAnDZu/GYNMg8oKDapg0uvi/DguFkrxbs3AtRHGWdONYXbGd1ZsGcVY9\n"
      + "DsCDR5R5+NCx8EEYfYSbz88dvncJMEq7iJiQXNdaj9dCHuZqaj5LGChBcLLldynX\n"
      + "mx3ZDE780aKPGomjeXEqcWgpeb0L4O+vGxkvz42C1XtvlsjBNPGKAjMM6xRPkorL\n"
      + "SfC1P0XyER3kqVYc4/cM9FyO7/vHLwH9byPCV4WbUpkti/bEtPs9xLnEtYP0oV30\n"
      + "PcdFVOg8hcuaEy6GoseU1EhlpgWJeBsbHMTlOB20JJa0kfFzREaJENyH6nHW3bSU\n"
      + "AwIDAQAB\n"
      + "-----END PUBLIC KEY-----\n";


  private KeyResolver resolver;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();
    ds.setUrl(sc.getInitParameter("db.url"));
    ds.setDriverClassName("org.h2.Driver");
    ds.setUsername(sc.getInitParameter("db.user"));
    ds.setPassword(sc.getInitParameter("db.password"));

    logger.info(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    CryptoService cs = new CryptoService();
    // Setting epid test mode enables epid signatures from debug and test
    // devices to pass validation. In production, this should never be used.
    cs.setEpidTestMode();
    logger.warn("*** WARNING ***");
    logger.warn("EPID Test mode enabled. This should NOT be enabled in production deployment.");

    sc.setAttribute("datasource", ds);
    sc.setAttribute("cryptoservice", cs);

    // To maintain backwards compatibility with installation without
    // any OnDie settings or installations that do not wish to use
    // OnDie we will check if the one required setting is present.
    // If not then the ods object is set to null and operation should
    // proceed without error. If an OnDie operation is attempted then
    // an error will occur at that time and the user will need to
    // correct their configuration.
    OnDieService initialOds = null;
    if (sc.getInitParameter("ods.cacheDir") != null
            && !sc.getInitParameter("ods.cacheDir").isEmpty()) {

      try {
        OnDieCache odc = new OnDieCache(
                URI.create(sc.getInitParameter("ods.cacheDir")),
                sc.getInitParameter("ods.autoUpdate").toLowerCase().equals("true"),
                sc.getInitParameter("ods.zipArtifactUrl"),
                null);

        odc.initializeCache();

        initialOds = new OnDieService(odc,
                sc.getInitParameter("ods.checkRevocations").equals("true"));

      } catch (Exception ex) {
        throw new RuntimeException("OnDie initialization error: " + ex.getMessage());
      }
    }

    final OnDieService ods = initialOds;

    sc.setAttribute("onDieService", ods);

    resolver = new KeyResolver() {
      @Override
      public PrivateKey getKey(PublicKey key) {
        String pemValue = ownerKeyPemEC256;
        if (key instanceof ECKey) {
          int bitLength = ((ECKey) key).getParams().getCurve().getField().getFieldSize();
          if (bitLength == Const.BIT_LEN_256) {
            pemValue = ownerKeyPemEC256;
          } else if (bitLength == Const.BIT_LEN_384) {
            pemValue = ownerKeyPemEC384;
          }
        } else if (key instanceof RSAKey) {
          pemValue = ownerKeyPemRsa;
        }
        return PemLoader.loadPrivateKey(pemValue);
      }
    };

    MessageDispatcher dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return createTo2Service(cs, ds, ods);
      }

      @Override
      protected void replied(Composite reply) {
        String msgId = reply.getAsNumber(Const.SM_MSG_ID).toString();
        logger.info("msg/" + msgId + ": " + reply.toString());
      }

      @Override
      protected void dispatching(Composite request) {
        String msgId = request.getAsNumber(Const.SM_MSG_ID).toString();
        logger.info("msg/" + msgId + ": " + request.toString());
      }

      @Override
      protected void failed(Exception e) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
          logger.warn("Failed to write data: " + e.getMessage());
        }
        logger.warn(writer.toString());
      }
    };
    sc.setAttribute(Const.DISPATCHER_ATTRIBUTE, dispatcher);
    //create tables
    OwnerDbManager manager = new OwnerDbManager();
    manager.createTables(ds);


    //create default customer the owners default day
    StringBuilder keyBuilder = new StringBuilder();

    keyBuilder.append(ownerPublicKeyPemEC256);
    keyBuilder.append(ownerPublicKeyPemEC384);
    keyBuilder.append(ownerPublicKeysPemRsa);
    manager.addCustomer(ds,1,"default",keyBuilder.toString());
    manager.loadTo2Settings(ds);

    // if the optional "voucher" file is present then use
    // it. If not, then fall back to the hardcoded value.
    try {
      FileInputStream fis = new FileInputStream("voucher");
      Composite voucher = Composite.fromObject(fis.readAllBytes());

      // select the owner key based on key type in the voucher
      PublicKey devicePubkey = cs.getDevicePublicKey(voucher);
      String pemValue = ownerKeyPemEC256;
      if (devicePubkey instanceof ECKey) {
        int bitLength = ((ECKey) devicePubkey).getParams().getCurve().getField().getFieldSize();
        if (bitLength == Const.BIT_LEN_256) {
          pemValue = ownerKeyPemEC256;
        } else if (bitLength == Const.BIT_LEN_384) {
          pemValue = ownerKeyPemEC384;
        }
      } else if (devicePubkey == null) {
        pemValue = ownerKeyPemRsa;
      } else {
        throw new RuntimeException("unknown public key type in voucher");
      }

      manager.addCustomer(ds, 1, "owner", pemValue);
      manager.addCustomer(ds, 2, "owner2", pemValue);
      manager.importVoucher(ds, voucher);

    } catch (Exception ex) {
      manager.addCustomer(ds, 1, "owner", ownerKeyPemEC256);
      manager.addCustomer(ds, 2, "owner2", ownerKeyPemEC256);
      manager.importVoucher(ds, Composite.fromObject(sampleVoucher));
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }

  private To2ServerService createTo2Service(CryptoService cs,
      DataSource ds,
      OnDieService ods) {
    return new To2ServerService() {
      private To2ServerStorage storage;

      @Override
      public To2ServerStorage getStorage() {
        if (storage == null) {
          storage = new OwnerDbStorage(cs, ds, resolver, ods);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }

}
