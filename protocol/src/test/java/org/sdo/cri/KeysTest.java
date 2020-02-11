// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.junit.jupiter.api.Test;

class KeysTest {

  // Curve named by OID in key
  private static final String ECDSA_PEM_0 = "-----BEGIN CERTIFICATE-----\n"
      + "MIIB0zCCAXqgAwIBAgIJAOvviqtFbmFyMAoGCCqGSM49BAMCMEUxCzAJBgNVBAYT\n"
      + "AkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBXaWRn\n"
      + "aXRzIFB0eSBMdGQwHhcNMTkwMzE4MTk0OTU1WhcNNDYwODAyMTk0OTU1WjBFMQsw\n"
      + "CQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50ZXJu\n"
      + "ZXQgV2lkZ2l0cyBQdHkgTHRkMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8g7i\n"
      + "9GB4LP5BSuKw/xho3vWauYY3bjmiCRBAV5ohWfaLBMBPcb4qvcrgJKvQ3ZidQVG+\n"
      + "oYcSxRyuARc3it4iVaNTMFEwHQYDVR0OBBYEFDRZ2E4YnCz6yJOzSmV42SAYfVIK\n"
      + "MB8GA1UdIwQYMBaAFDRZ2E4YnCz6yJOzSmV42SAYfVIKMA8GA1UdEwEB/wQFMAMB\n"
      + "Af8wCgYIKoZIzj0EAwIDRwAwRAIgCBfNoXxvu6YuSo4BMCPT+UEeLLhsU1NQ1pds\n"
      + "vNDwQroCIBp3p+SNprXcCPcVFRArICXEZKHZMYg0o+YPjcOCHEM/\n"
      + "-----END CERTIFICATE-----";

  // Curve parameters supplied explicitly
  private static final String ECDSA_PEM_1 = "-----BEGIN CERTIFICATE-----\n"
      + "MIICxzCCAm6gAwIBAgIJAJPJ7XknmRGQMAoGCCqGSM49BAMCMEUxCzAJBgNVBAYT\n"
      + "AkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBXaWRn\n"
      + "aXRzIFB0eSBMdGQwHhcNMTkwMzE4MTgzNjM4WhcNNDYwODAyMTgzNjM4WjBFMQsw\n"
      + "CQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50ZXJu\n"
      + "ZXQgV2lkZ2l0cyBQdHkgTHRkMIIBSzCCAQMGByqGSM49AgEwgfcCAQEwLAYHKoZI\n"
      + "zj0BAQIhAP////8AAAABAAAAAAAAAAAAAAAA////////////////MFsEIP////8A\n"
      + "AAABAAAAAAAAAAAAAAAA///////////////8BCBaxjXYqjqT57PrvVV2mIa8ZR0G\n"
      + "sMxTsPY7zjw+J9JgSwMVAMSdNgiG5wSTamZ44ROdJreBn36QBEEEaxfR8uEsQkf4\n"
      + "vOblY6RA8ncDfYEt6zOg9KE5RdiYwpZP40Li/hp/m47n60p8D54WK84zV2sxXs7L\n"
      + "tkBoN79R9QIhAP////8AAAAA//////////+85vqtpxeehPO5ysL8YyVRAgEBA0IA\n"
      + "BKFs6R/bSStK8DHNsHybip2YFeuzwYNOoyoyqIcH6B0VekjsETyOlKD/uNTgViVh\n"
      + "eGzBoKJRZhtyg9k1wfs5OQ+jUzBRMB0GA1UdDgQWBBTWzdTN+fZgHW/qW1v94+Fp\n"
      + "/XrZeTAfBgNVHSMEGDAWgBTWzdTN+fZgHW/qW1v94+Fp/XrZeTAPBgNVHRMBAf8E\n"
      + "BTADAQH/MAoGCCqGSM49BAMCA0cAMEQCIB0rtPGabhkD9zWWdZNvqgEwVD6lG/Lw\n"
      + "Pm+ppOxCKNc9AiARISek2K7MuQp1QwPPtzxxgrVdCREsZshT7o0IWEEQiw==\n"
      + "-----END CERTIFICATE-----";

  @Test
  void toType() {

    final JcaX509CertificateConverter cc = new JcaX509CertificateConverter();
    cc.setProvider(new BouncyCastleProvider());

    for (String s : Arrays.asList(ECDSA_PEM_0, ECDSA_PEM_1)) {
      assertDoesNotThrow(() -> {
        try (StringReader rdr = new StringReader(s); PEMParser p = new PEMParser(rdr)) {
          for (Object o = p.readObject(); null != o; o = p.readObject()) {
            if (o instanceof X509CertificateHolder) {
              final X509CertificateHolder certHolder = (X509CertificateHolder) o;
              final Certificate cert = cc.getCertificate(certHolder);
              final PublicKey key = cert.getPublicKey();
              assertNotNull(Keys.toType(key));
            }
          }
        }
      });
    }
  }
}
