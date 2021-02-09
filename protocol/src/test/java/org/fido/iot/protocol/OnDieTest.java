// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import org.fido.iot.protocol.ondie.OnDieCache;
import org.fido.iot.protocol.ondie.OnDieService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class OnDieTest {

  OnDieCache onDieCache = null;

  String b64TestSignature =
          "ARDiywa9EaMjQZ0dNWO4CbxGEL0vujai1k2rk5D/baL+8xwBsQ4ZF/eL0V/yxtaafl11BJZ7rjnesm"
        + "/H8i6Hq3r8DeObqqGDo88mVnibvb9z3zlYlLahzLkwkhxsoTRRzXIQ6km2Dm6hQX5zmRkUDiFtzadw"
        + "MDfh+dPVQMlf/vNG1j5K";
  String serialNo = "daltest";
  String b64DeviceCert =
          "MIIBszCCATqgAwIBAgIQcYhLQDPbPylyGiZ0lFRLwzAKBggqhkjOPQQDAzAeMRwwGgYDVQQDDBNDU0"
        + "1FIFRHTCBEQUxfSTAxU0RFMB4XDTE5MDEwMTAwMDAwMFoXDTQ5MTIzMTIzNTk1OVowFzEVMBMGA1UE"
        + "AwwMREFMIEludGVsIFRBMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE044GJ2MiK44UHXubptTvkGefiy"
        + "rKO9ofn5v1yBVJcwpbYYTBjop/W01f7Gv7se7sMin8D1zfoEIQuahlijcsVWlG0CcB6LodLkxQi+IS"
        + "D8MNbObYIt8EGIacVVOgPdSho0QwQjAfBgNVHSMEGDAWgBSuPjAqQWKsFmeOf7U8OWyMbE+tfTAPBg"
        + "NVHRMBAf8EBTADAQEAMA4GA1UdDwEB/wQEAwIDyDAKBggqhkjOPQQDAwNnADBkAjAdss2kczBguN6s"
        + "iidupV+ipN8bCVAYe3eZV7c3i9rhTpHipVdII1/ppdswzl2IXQ0CMHNeOFuvHe64S9m2JRbBXUSdJ7"
        + "iNQwp/4+OdQUmWYs2mB7KqZpmDPGQkq5mDuygBaA==";


  OnDieCache getTestOnDieCache() throws Exception {
    if (onDieCache == null) {
      onDieCache = new OnDieCache(
              getClass().getClassLoader().getResource("cachedir").getFile(),
              false,
              null,
              null);
    }
    return onDieCache;
  }


  PublicKey getPublicKey() throws CertificateException {
    // get public key from cert path
    byte [] certBytes = Base64.getDecoder().decode(b64DeviceCert);
    CertificateFactory certificateFactory =
            CertificateFactory.getInstance("X.509"); //TODO, BouncyCastleLoader.load());
    Certificate cert = certificateFactory.generateCertificate(
            new ByteArrayInputStream(certBytes));
    return cert.getPublicKey();
  }

  // The following test is left disabled since it can fail depending on
  // the test environment. The test requires access to the source URLs for
  // OnDie certs and CRLs. If this test is run on a network with access then
  // it will fail. We cannot guarantee that this will be true for all environments.
  // Comment out the @Disabled if you want to run the test.
  @Test
  @DisplayName("OnDie cache download test")
  @Disabled
  void testOnDieCacheDownload(@TempDir Path tempDir) throws Exception {

    try {
      String sourceList = "https://tsci.intel.com/content/OnDieCA/certs/"
        + "https://tsci.intel.com/content/OnDieCA/crls/";

      OnDieCache onDieCache = new OnDieCache(
              tempDir.toString(),
              true,
              sourceList,
              null);

      assertNotNull(onDieCache.getCertOrCrl(
        "https://pre1-tsci.intel.com/content/OD/certs/TGL_00001846_OnDie_CA.crl"));
      assertNull(onDieCache.getCertOrCrl(
        "https://pre1-tsci.intel.com/content/OD/certs/NOT_IN_THE_CACHE.crl"));
      assertThrows(MalformedURLException.class,
        () -> onDieCache.getCertOrCrl("TGL_00001846_OnDie_CA.crl"));
    } catch (Exception ex) {
      throw ex;
    } finally {
    }
  }

  @Test
  @DisplayName("OnDie cache load test")
  void testOnDieCacheLoad() throws Exception {
    OnDieCache onDieCache = getTestOnDieCache();

    assertNotNull(onDieCache.getCertOrCrl(
      "https://pre1-tsci.intel.com/content/OD/certs/TGL_00001846_OnDie_CA.crl"));
    assertThrows(MalformedURLException.class,
      () -> onDieCache.getCertOrCrl("TGL_00001846_OnDie_CA.crl"));
  }

  @Test
  @DisplayName("OnDie invalid signed data test")
  void testOnDieSignatureInvalidSignedData() throws Exception {

    OnDieCache onDieCache = getTestOnDieCache();
    OnDieService ods = new OnDieService(onDieCache, false);

    // modify the signed data and verify signature fails
    assertFalse(ods.validateSignature(
            getPublicKey(),
            (serialNo + "extra data").getBytes(),
            Base64.getDecoder().decode(b64TestSignature)
            ));
  }

  @Test
  @DisplayName("OnDie invalid signature data test")
  void testOnDieSignatureInvalidSignatureData() throws Exception {

    OnDieCache onDieCache = getTestOnDieCache();
    OnDieService ods = new OnDieService(onDieCache, false);

    // modify the signature data and verify signature fails
    assertFalse(ods.validateSignature(
            getPublicKey(),
            serialNo.getBytes(),
            Base64.getDecoder().decode(b64TestSignature.substring(1))
      ));
  }

  @Test
  @DisplayName("OnDie signature test")
  void testOnDieSignature() throws Exception {

    OnDieCache onDieCache = getTestOnDieCache();
    OnDieService ods = new OnDieService(onDieCache, false);

    assertTrue(ods.validateSignature(
            getPublicKey(),
            serialNo.getBytes(),
            Base64.getDecoder().decode(b64TestSignature)));
  }

  @Test
  @DisplayName("OnDie signature test with revocations")
  void testOnDieSignatureWithRevocations() throws Exception {

    OnDieCache onDieCache = getTestOnDieCache();
    OnDieService ods = new OnDieService(onDieCache, false);

    String b64CertPath = "[1,7,[[606,\"MIICWjCCAd+gAwIBAgIQRi3M/aBkpNPGV8V9KVkwxjAKBggqhkjOPQQDAzAeMRwwGgYDVQQDDBNDU01FIE1DQyBEQUxfSTAxU1ZOMB4XDTIwMTIyMzAwMDAwMFoXDTQ5MTIzMTIzNTk1OVowFzEVMBMGA1UEAwwMREFMIEludGVsIFRBMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE89gWlNjTOPGdxGtZVMNu1RJ2pm0G0+2KzNBAeUbu1S1Kcg0q110jGB/3VtjFypUgfpSy5huacgF5R6xeY/PN67Gvt59T2oDtVTqjlKImDx9GoBEC1uA2bI8aEqNIuWBWo4HoMIHlMB8GA1UdIwQYMBaAFHEK+C7yu1Ewfl7sO1ClE3RJ6UohMA8GA1UdEwEB/wQFMAMBAQAwDgYDVR0PAQH/BAQDAgPIMIGgBgNVHR8EgZgwgZUwgZKgSqBIhkZodHRwczovL3RzY2kuaW50ZWwuY29tL2NvbnRlbnQvT25EaWVDQS9jcmxzL09uRGllX0NBX0NTTUVfSW5kaXJlY3QuY3JsokSkQjBAMSYwJAYDVQQLDB1PbkRpZSBDQSBDU01FIEludGVybWVkaWF0ZSBDQTEWMBQGA1UEAwwNd3d3LmludGVsLmNvbTAKBggqhkjOPQQDAwNpADBmAjEA9wI6SVdBckvPGp09w6snbb9XDTXEaYNhZkGN8s5sW8dI0rCp17jp9Sje2ZGBoIukAjEAvOgFMZb16TbePCKBaEMjNJQQyfFdwm8PjkrSGoHGrxt909ZcPsY/VWqBBISgIVn+\"],[638,\"MIICejCCAgGgAwIBAgIFAQQUAEAwCgYIKoZIzj0EAwMwIzEhMB8GA1UEAwwYQ1NNRSBNQ0MgU1ZOMDEgS2VybmVsIENBMB4XDTIwMTIyMzAwMDAwMFoXDTQ5MTIzMTIzNTk1OVowHjEcMBoGA1UEAwwTQ1NNRSBNQ0MgREFMX0kwMVNWTjB2MBAGByqGSM49AgEGBSuBBAAiA2IABFdldvsAK7XgqjkdM4FY3RVtMAnyJKLm+YPTt7Br/PIoE0vutTJ/WB+Ua0aVu46EMAGw8yR73KP4jkofDT/1wz6jVxIT9eclGn21dLAVfvumCpw0FiqKFgeK/V+S6iy7eKOCAQgwggEEMB8GA1UdIwQYMBaAFC04wQ4+zQD48uiIIHA/XksmRSsBMB0GA1UdDgQWBBRxCvgu8rtRMH5e7DtQpRN0SelKITAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwICBDCBoAYDVR0fBIGYMIGVMIGSoEqgSIZGaHR0cHM6Ly90c2NpLmludGVsLmNvbS9jb250ZW50L09uRGllQ0EvY3Jscy9PbkRpZV9DQV9DU01FX0luZGlyZWN0LmNybKJEpEIwQDEmMCQGA1UECwwdT25EaWUgQ0EgQ1NNRSBJbnRlcm1lZGlhdGUgQ0ExFjAUBgNVBAMMDXd3dy5pbnRlbC5jb20wCgYIKoZIzj0EAwMDZwAwZAIwAoeyPLJxXXxRXS/NYgbeEUg4jENTJjHV8H3TSlEvtDJo28nzA0VFL9/2rYXpb2pBAjBGB3m1hoJbBmo4mKO3OVY7zGwdyZVTktH3va6OXrI+usVd59Qerfd4L+gkQWOykBI=\"],[631,\"MIICczCCAfmgAwIBAgIBATAKBggqhkjOPQQDAzAaMRgwFgYDVQQDDA9DU01FIE1DQyBST00gQ0EwHhcNMjAxMjIzMDAwMDAwWhcNNDkxMjMxMjM1OTU5WjAjMSEwHwYDVQQDDBhDU01FIE1DQyBTVk4wMSBLZXJuZWwgQ0EwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAARZLZSlm9VRvRETI4YGocA0rFg0UguTZXCJNzUa5G7PIQBa6X67Gj4PS5uJAb9Z9J0QXxOD8X8h3Hzj3MFG8AyVqtkTMUmLHOLu14JVWXGpw9ArdiD0WK+8yPr+gkaj+HGjggEIMIIBBDAfBgNVHSMEGDAWgBRVl6pS1ZoXIqr6ufsop5R/SSSK3DAdBgNVHQ4EFgQULTjBDj7NAPjy6IggcD9eSyZFKwEwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAqwwgaAGA1UdHwSBmDCBlTCBkqBKoEiGRmh0dHBzOi8vdHNjaS5pbnRlbC5jb20vY29udGVudC9PbkRpZUNBL2NybHMvT25EaWVfQ0FfQ1NNRV9JbmRpcmVjdC5jcmyiRKRCMEAxJjAkBgNVBAsMHU9uRGllIENBIENTTUUgSW50ZXJtZWRpYXRlIENBMRYwFAYDVQQDDA13d3cuaW50ZWwuY29tMAoGCCqGSM49BAMDA2gAMGUCMDaytWfVNtvXrwrnOcIKwounoyIOT0dseWeAg0y0l6Objltxw+qc5W32jq6gAXFjWgIxAM4Fbp1Nj6hCpZSpJjx2VEONJhVXx0aKtOdviqCHBLPsHijrycB8V8OCJVW00tZY/A==\"],[708,\"MIICwDCCAkagAwIBAgIQW81xpHBhMGgxpdzkKaJ/ZjAKBggqhkjOPQQDAzBIMS4wLAYDVQQLDCVPbiBEaWUgQ1NNRSBQX01DQyAwMDAwMTg4MSBJc3N1aW5nIENBMRYwFAYDVQQDDA13d3cuaW50ZWwuY29tMB4XDTE5MDUwMTAwMDAwMFoXDTQ5MTIzMTIzNTk1OVowGjEYMBYGA1UEAwwPQ1NNRSBNQ0MgUk9NIENBMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEOtXDWztz+JRyJ40uRtxTzcgv6dgsLETCkuOWy4AyfNsk5gXhYyANHbNwUQ4k631Of5P35XPeEOjrHLAcymOqS4b82asUm15dzv3MUic1sx8NoQ0295dl7oaEE5Q3tsnRo4IBITCCAR0wHwYDVR0jBBgwFoAU8we6MzR8KDoeDBUIrQL1yMoG9F4wHQYDVR0OBBYEFFWXqlLVmhciqvq5+yinlH9JJIrcMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgGuMGIGCCsGAQUFBwEBBFYwVDBSBggrBgEFBQcwAoZGaHR0cHM6Ly90c2NpLmludGVsLmNvbS9jb250ZW50L09uRGllQ0EvY2VydHMvTUNDXzAwMDAxODgxX09uRGllX0NBLmNlcjBWBgNVHR8ETzBNMEugSaBHhkVodHRwczovL3RzY2kuaW50ZWwuY29tL2NvbnRlbnQvT25EaWVDQS9jcmxzL01DQ18wMDAwMTg4MV9PbkRpZV9DQS5jcmwwCgYIKoZIzj0EAwMDaAAwZQIwVQ9WlDGvWCgLx3P2kLUdcOKLWscyG5SQwcmzrf2hH1dYXMgbS9N9isaiOYvC6QfXAjEApYLaXSsdpTBUXXHVxMS5qD3cAQr07QlmBkrxVHF0Z7k1yfGjrP4rk2hJafgEqAQn\"],[756,\"MIIC8DCCAnWgAwIBAgIUZi+xu0T2Z9Z3QmBZ6iOovdIWCc4wCgYIKoZIzj0EAwMwQDEmMCQGA1UECwwdT25EaWUgQ0EgQ1NNRSBJbnRlcm1lZGlhdGUgQ0ExFjAUBgNVBAMMDXd3dy5pbnRlbC5jb20wHhcNMTkwNTAxMDAwMDAwWhcNNDkxMjMxMjM1OTU5WjBIMS4wLAYDVQQLDCVPbiBEaWUgQ1NNRSBQX01DQyAwMDAwMTg4MSBJc3N1aW5nIENBMRYwFAYDVQQDDA13d3cuaW50ZWwuY29tMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEAyt8PUvNaHB0Z+a8lZbzruMMTSZRdAPbyP5rso75X8zD8bRaLd2Imj/NsqfnkcweLFjp/DSQrDErBOy90DBzLnvLQUv5tcm7nBmuYudcnZHFmTYWm8p1aPy3lS9r6IoCo4IBJjCCASIwHQYDVR0OBBYEFPMHujM0fCg6HgwVCK0C9cjKBvReMB8GA1UdIwQYMBaAFGGaCniWiAPMYZCopT+QbXV8n2lqMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgEGMGcGCCsGAQUFBwEBBFswWTBXBggrBgEFBQcwAoZLaHR0cHM6Ly90c2NpLmludGVsLmNvbS9jb250ZW50L09uRGllQ0EvY2VydHMvT25EaWVfQ0FfQ1NNRV9JbnRlcm1lZGlhdGUuY2VyMFYGA1UdHwRPME0wS6BJoEeGRWh0dHBzOi8vdHNjaS5pbnRlbC5jb20vY29udGVudC9PbkRpZUNBL2NybHMvT25EaWVfQ0FfQ1NNRV9Qcm9kdWN0LmNybDAKBggqhkjOPQQDAwNpADBmAjEAjz1UnT4SgJglAgJtSvpQeIrtmNHeA03tlwNxgGoTm+8jPLd/59E/2mwIZT2eeQ2ZAjEA91DbMoWvfI0mYR0Y08hIx3A4C0+39WPcNv+uirNnBOn09EACxfVfWijYLFljXL0A\"],[821,\"MIIDMTCCAragAwIBAgIQQAAAAAAAAAAAAAAAAAAAADAKBggqhkjOPQQDAzCBkjELMAkGA1UEBgwCVVMxCzAJBgNVBAgMAkNBMRQwEgYDVQQHDAtTYW50YSBDbGFyYTEaMBgGA1UECgwRSW50ZWwgQ29ycG9yYXRpb24xLDAqBgNVBAsMI09uRGllIENBIERFQlVHIFJvb3QgQ2VydCBTaWduaW5nIENBMRYwFAYDVQQDDA13d3cuaW50ZWwuY29tMB4XDTE5MDEwMTAwMDAwMFoXDTQ5MTIzMTIzNTk1OVowQDEmMCQGA1UECwwdT25EaWUgQ0EgQ1NNRSBJbnRlcm1lZGlhdGUgQ0ExFjAUBgNVBAMMDXd3dy5pbnRlbC5jb20wdjAQBgcqhkjOPQIBBgUrgQQAIgNiAAR49jkb/ywtjtgP0m03S0uhfB3eV57XH/HzyHDoOgA2MYQjmy173mBgD4QFT/TP6moPnMQbsuqDsVfoGOZj2OnxAcKVAcTqPB3pr3Svf9tlmmmkq3bryyZFDY6+0dRrCnajggEgMIIBHDAdBgNVHQ4EFgQUUDpOob4/l7oyhn/ieL3t7mmHLNIwHwYDVR0jBBgwFoAU78+n4qeCW0zN6tR0Uj/2uZpliEowDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwbgYIKwYBBQUHAQEEYjBgMF4GCCsGAQUFBzAChlJodHRwczovL3RzREUuaW50ZWwuY29tL2NvbnRlbnQvT25EaWVDQS9jZXJ0cy9PbkRpZV9DQV9ERUJVR19Sb290Q0FfQ2VydGlmaWNhdGUuY2VyMEkGA1UdHwRCMEAwPqA8oDqGOGh0dHBzOi8vdHNERS5pbnRlbC5jb20vY29udGVudC9PbkRpZUNBL2NybHMvT25EaWVfREUuY3JsMAoGCCqGSM49BAMDA2kAMGYCMQDDjm/4RtKbS6xu3ZiLz4ymWc9+jf4K5JYvDp+K/Md44z1/Ry/I4Vf5r+CSaKFElh0CMQCRgBB4M8r/MeiB3ni2J2qFkBG8H7rTK09W1BQf/I931SI2JjcgooVp5rOC7qIzs4I=\"],[712,\"MIICxDCCAkqgAwIBAgIQQAAAAAAAAAAAAAAAAAAAADAKBggqhkjOPQQDAzCBkjELMAkGA1UEBgwCVVMxCzAJBgNVBAgMAkNBMRQwEgYDVQQHDAtTYW50YSBDbGFyYTEaMBgGA1UECgwRSW50ZWwgQ29ycG9yYXRpb24xLDAqBgNVBAsMI09uRGllIENBIERFQlVHIFJvb3QgQ2VydCBTaWduaW5nIENBMRYwFAYDVQQDDA13d3cuaW50ZWwuY29tMB4XDTE5MDEwMTAwMDAwMFoXDTQ5MTIzMTIzNTk1OVowgZIxCzAJBgNVBAYMAlVTMQswCQYDVQQIDAJDQTEUMBIGA1UEBwwLU2FudGEgQ2xhcmExGjAYBgNVBAoMEUludGVsIENvcnBvcmF0aW9uMSwwKgYDVQQLDCNPbkRpZSBDQSBERUJVRyBSb290IENlcnQgU2lnbmluZyBDQTEWMBQGA1UEAwwNd3d3LmludGVsLmNvbTB2MBAGByqGSM49AgEGBSuBBAAiA2IABL8ArWuvvgynyq4Es77WtPZ9i0k8WN1sX23eqaddu0fD66fVqg+Otu7SVG2AV1w9P+j2zr1ZESDlDcKPbOvbok54jJjiUA8+8JeNXr6Hbi/0BKs+o+jg4zl6BTqPifahsKNjMGEwHQYDVR0OBBYEFO/Pp+KngltMzerUdFI/9rmaZYhKMB8GA1UdIwQYMBaAFO/Pp+KngltMzerUdFI/9rmaZYhKMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgEGMAoGCCqGSM49BAMDA2gAMGUCMQDGBQnGuNCqgRi5z+bYS3CefF4zPZH6/g8rRYEbT0KFFOCsCfoH3Y/ZZI7aRoX6RlECMCkmB7LOssgXkI553HSfxKUoVpWWo91Q/sDelgx4vI7ezeNG9tVePSoFZmZggF+Cmg==\"]]]";

    String signedData = "{\"ai\":[16,0,\"4ssGvRGjI0GdHTVjuAm8Rg==\"],\"n6\":\"7mzy6d8faVIewE3OkvKONQ==\",\"n7\":\"9ZMKASN4QJliW7W+l6boVg==\",\"g2\":\"nOQq8eKHQTq1dHM23nrzoA==\",\"nn\":1,\"xB\":[256,\"j8FBpUY1idsZN92hm5Xr6JRFTi+t0huDmTym+hTfUjqfdD0UF7e8ih0bDEIy5A2eYIbpu0Y3b0KySL6wxUSqNduWsQdwLgfeEtRzFIi8vPGyAUAMEIvTABv4AI8Hq870rvKK4ggoDzi533z01YFBra7Sb649K5O7CyEg4BP2nGanMshr9Te+xTP919byCRjXWvcnCcVYrdzY2bUPCmGkLztDXMTwEeVtYFR/ehhLYCmX3fKfVWA6JDB3V/zAh69ETBYL0PmFSCXeU69ztDo+qppe5RH54uCXqYF8Kf7TOO8i1Sz5996uOQ9BZqKxbzf7tlIaiAMkr2iZZXdOdos/EA==\"]}";
    String b64Signature = "ARDiywa9EaMjQZ0dNWO4CbxGEL0vujai1k2rk5D/baL+8xwBgqQJSXhX3epmp2iUTDwfmt9OB8DPLFQaUol8YSKG7T7qpFS8VsVPNoSrLAUBTRLTOasNRIDM64utKHZmON39enxucC7LVPs+7nwu1qWtdmd5M5WmI6Q4HBr/TTzAEvZr";

    /*
    CharBuffer certPathBuf = CharBuffer.wrap(b64CertPath);
    Codec<CertPath>.Decoder dcDec = new CertPathCodec().decoder();
    CertPath certPath = dcDec.apply(certPathBuf);

    assertTrue(ods.validateSignature(
            (List<Certificate>) certPath.getCertificates(),
            signedData.getBytes(),
            Base64.getDecoder().decode(b64Signature),
            true));
  */
  }
}
