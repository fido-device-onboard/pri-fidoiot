// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.ondie;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.util.encoders.Base64;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Const;


public class OnDieService {

  private boolean checkRevocations = true;

  private OnDieCache onDieCache;

  static final int taskInfoLength = 36;  // length of the taskinfo part of OnDie signature
  static final int rLength = 48;  // length of the r field part of OnDie signature
  static final int sLength = 48;  // length of the s field part of OnDie signature

  private static final String b64RootCa =
        "MIICujCCAj6gAwIBAgIUPLLiHTrwySRtWxR4lxKLlu7MJ7wwDAYIKoZIzj0EAwMFADCBiTELMAkGA1UEBgwCVVMxCz"
      + "AJBgNVBAgMAkNBMRQwEgYDVQQHDAtTYW50YSBDbGFyYTEaMBgGA1UECgwRSW50ZWwgQ29ycG9yYXRpb24xIzAhBgNV"
      + "BAsMGk9uRGllIENBIFJvb3QgQ2VydCBTaWduaW5nMRYwFAYDVQQDDA13d3cuaW50ZWwuY29tMB4XDTE5MDQwMzAwMD"
      + "AwMFoXDTQ5MTIzMTIzNTk1OVowgYkxCzAJBgNVBAYMAlVTMQswCQYDVQQIDAJDQTEUMBIGA1UEBwwLU2FudGEgQ2xh"
      + "cmExGjAYBgNVBAoMEUludGVsIENvcnBvcmF0aW9uMSMwIQYDVQQLDBpPbkRpZSBDQSBSb290IENlcnQgU2lnbmluZz"
      + "EWMBQGA1UEAwwNd3d3LmludGVsLmNvbTB2MBAGByqGSM49AgEGBSuBBAAiA2IABK8SfB2UflvXZqb5Kc3+lokrABHW"
      + "azvNER2axPURP64HILkXChPB0OEX5hLB7Okw7Dy6oFqB5tQVDupgfvUX/SgYBEaDdG5rCVFrGAis6HX5TA2ewQmj14"
      + "r2ncHBgnppB6NjMGEwHwYDVR0jBBgwFoAUtFjJ9uQIQKPyWMg5eG6ujgqNnDgwDwYDVR0TAQH/BAUwAwEB/zAOBgNV"
      + "HQ8BAf8EBAMCAQYwHQYDVR0OBBYEFLRYyfbkCECj8ljIOXhuro4KjZw4MAwGCCqGSM49BAMDBQADaAAwZQIxAP9B4l"
      + "FF86uvpHmkcp61cWaU565ayE3p7ezu9haLE/lPLh5hFQfmTi1nm/sG3JEXMQIwNpKfHoDmUTrUyezhhfv3GG+1CqBX"
      + "stmCYH40buj9jKW3pHWc71s9arEmPWli7I8U";

  private static final String b64DebugRootCa =
        "MIICxDCCAkqgAwIBAgIQQAAAAAAAAAAAAAAAAAAAADAKBggqhkjOPQQDAzCBkjELMAkGA1UEBgwCVVMxCzAJBgNV"
      + "BAgMAkNBMRQwEgYDVQQHDAtTYW50YSBDbGFyYTEaMBgGA1UECgwRSW50ZWwgQ29ycG9yYXRpb24xLDAqBgNVBAsM"
      + "I09uRGllIENBIERFQlVHIFJvb3QgQ2VydCBTaWduaW5nIENBMRYwFAYDVQQDDA13d3cuaW50ZWwuY29tMB4XDTE5"
      + "MDEwMTAwMDAwMFoXDTQ5MTIzMTIzNTk1OVowgZIxCzAJBgNVBAYMAlVTMQswCQYDVQQIDAJDQTEUMBIGA1UEBwwL"
      + "U2FudGEgQ2xhcmExGjAYBgNVBAoMEUludGVsIENvcnBvcmF0aW9uMSwwKgYDVQQLDCNPbkRpZSBDQSBERUJVRyBS"
      + "b290IENlcnQgU2lnbmluZyBDQTEWMBQGA1UEAwwNd3d3LmludGVsLmNvbTB2MBAGByqGSM49AgEGBSuBBAAiA2IA"
      + "BL8ArWuvvgynyq4Es77WtPZ9i0k8WN1sX23eqaddu0fD66fVqg+Otu7SVG2AV1w9P+j2zr1ZESDlDcKPbOvbok54"
      + "jJjiUA8+8JeNXr6Hbi/0BKs+o+jg4zl6BTqPifahsKNjMGEwHwYDVR0jBBgwFoAU78+n4qeCW0zN6tR0Uj/2uZpl"
      + "iEowHQYDVR0OBBYEFO/Pp+KngltMzerUdFI/9rmaZYhKMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgEG"
      + "MAoGCCqGSM49BAMDA2gAMGUCMQDJMFhMuTUzKZ1fRIostdWHMh+a0SRMa0nFSpD4C4lWsdhXNCEUHRCoQp0zUnFM"
      + "ijICMFn9oVc47V+pcvEVgmjKrWOg1uDF5YAShTckzTvQPAB/UAQfJJXiXQjYbwtCCp/SAA==";

  private static byte[] rootCaBytes;
  private static byte[] rootDebugCaBytes;
  private static final LoggerService logger = new LoggerService(OnDieService.class);

  /**
   * Constructor.
   *
   * @param onDieCache onDieCache, required for revocation checking, optional otherwise
   * @param checkRevocations checkRevocations
   */
  public OnDieService(OnDieCache onDieCache, boolean checkRevocations) {
    this.onDieCache = onDieCache;
    this.checkRevocations = checkRevocations;
    if (checkRevocations && onDieCache == null) {
      throw new RuntimeException("OnDie error: OnDieCache is required for revocation checking");
    }

    rootCaBytes = Base64.decode(b64RootCa);
    rootDebugCaBytes = Base64.decode(b64DebugRootCa);
  }

  public OnDieCache getOnDieCache() {
    return this.onDieCache;
  }

  /**
   * Performs a validation of the given signature with the public key
   * extracted from the given cert chain.
   *
   * @param certChain certChain
   * @param signedData signedData
   * @param signature signature
   * @return boolean indicating if signature is valid.
   */
  public boolean validateSignature(List<Certificate> certChain,
                                   byte[] signedData,
                                   byte[] signature) {

    // Check revocations first.
    if ((certChain == null) || (checkRevocations && !checkRevocations(certChain))) {
      return false;
    }

    // validate that the cert chain is anchored to correct root CA
    try {
      if (!isRootCa(((X509Certificate) certChain.get(certChain.size() - 1)).getEncoded())) {
        return false;
      }
    } catch (CertificateEncodingException ex) {
      return false;
    }
    return validateSignature(certChain.get(0).getPublicKey(), signedData, signature);
  }

  /**
   * Performs a validation of the given signature with the public key
   * extracted from the given cert chain.
   *
   * @param verificationKey verificationKey for signature
   * @param signedData signedData
   * @param signature signature
   * @return boolean indicating if signature is valid.
   */
  public boolean validateSignature(PublicKey verificationKey,
                                   byte[] signedData,
                                   byte[] signature) {
    try {
      // check minimum length (taskinfo + R + S)
      if (signature.length < (taskInfoLength + rLength + sLength)) {
        return false;
      }
      byte[] taskInfo = Arrays.copyOfRange(signature, 0, taskInfoLength);

      // adjust the signed data
      // data-to-verify format is: [ task-info | nonce (optional) | data ]
      // First 36 bytes of signature is the taskinfo. This value must be prepended
      // to the signed data
      ByteArrayOutputStream adjSignedData = new ByteArrayOutputStream();
      adjSignedData.write(Arrays.copyOfRange(signature, 0, taskInfo.length));
      adjSignedData.write(signedData);

      byte[] adjSignature = convertSignature(signature, taskInfo);
      Signature sig = Signature.getInstance("SHA384withECDSA");

      sig.initVerify(verificationKey);
      sig.update(adjSignedData.toByteArray());
      return sig.verify(adjSignature);
    } catch (Exception ex) {
      return false;
    }
  }

  private static byte[] convertSignature(byte[] signature, byte[] taskInfo)
          throws IllegalArgumentException, IOException {
    if (taskInfo.length != 36) {
      throw new IllegalArgumentException("taskinfo length is incorrect: " + taskInfo.length);
    }

    final int rLength = 48;  // length of the r field part of OnDie signature
    final int sLength = 48;  // length of the s field part of OnDie signature

    // Format for signature should be as follows:
    // 0x30 b1 0x02 b2 (vr) 0x02 b3 (vs)
    // The b1 = length of remaining bytes,
    // b2 = length of R value (vr), b3 = length of S value (vs)
    byte[] rvalue = Arrays.copyOfRange(signature, taskInfo.length, taskInfo.length + rLength);
    byte[] svalue = Arrays.copyOfRange(signature,
            taskInfo.length + rLength,
            taskInfo.length + rLength + sLength);

    boolean appendZeroToR = false;
    boolean appendZeroToS = false;
    if ((rvalue[0] & 0x80) != 0) {
      appendZeroToR = true;
    }
    if ((svalue[0] & 0x80) != 0) {
      appendZeroToS = true;
    }

    ByteArrayOutputStream adjSignature = new ByteArrayOutputStream();
    adjSignature.write(0x30);
    // total length of remaining bytes
    adjSignature.write(4
            + (appendZeroToR ? rLength + 1 : rLength)
            + (appendZeroToS ? sLength + 1 : sLength));
    adjSignature.write(0x02);
    // R value
    if (appendZeroToR) {
      adjSignature.write(rLength + 1);
      adjSignature.write(0x00);
      adjSignature.write(rvalue);
    } else {
      adjSignature.write(rLength);
      adjSignature.write(rvalue);
    }
    adjSignature.write(0x02);
    // S value
    if (appendZeroToS) {
      adjSignature.write(sLength + 1);
      adjSignature.write(0x00);
      adjSignature.write(svalue);
    } else {
      adjSignature.write(sLength);
      adjSignature.write(svalue);
    }
    return adjSignature.toByteArray();
  }

  private boolean checkRevocations(List<Certificate> certificateList) {
    // Check revocations first.
    try {
      CertificateFactory certificateFactory = CertificateFactory.getInstance(Const.X509_ALG_NAME);
      for (Certificate cert: certificateList) {
        X509Certificate x509cert = (X509Certificate) cert;
        X509CertificateHolder certHolder = new X509CertificateHolder(x509cert.getEncoded());
        CRLDistPoint cdp = CRLDistPoint.fromExtensions(certHolder.getExtensions());
        if (cdp != null) {
          DistributionPoint[] distPoints = cdp.getDistributionPoints();
          for (DistributionPoint dp : distPoints) {
            GeneralName[] generalNames =
                    GeneralNames.getInstance(dp.getDistributionPoint().getName()).getNames();
            for (GeneralName generalName : generalNames) {
              String name = generalName.toString();
              byte[] crlBytes = onDieCache.getCertOrCrl(name.substring(name.indexOf("http")));
              if (crlBytes == null) {
                logger.info("CRL: " + generalName.getName().toString()
                        + " not found in cache for cert: "
                        + x509cert.getIssuerX500Principal().getName());
                return false;
              } else {
                CRL crl = certificateFactory.generateCRL(new ByteArrayInputStream(crlBytes));
                if (crl.isRevoked(cert)) {
                  return false;
                }
              }
            }
          }
        }
      }
    } catch (IOException | CertificateException | CRLException ex) {
      return false;
    }
    return true;
  }

  /**
   * Identifies whether the given cert matches the OnDie root CA certs.
   *
   * @param caBytes certificate to compare with
   * @return true if matches any one of OnDie root CA certs
   */
  public boolean isRootCa(byte[] caBytes) {
    if (Arrays.equals(caBytes, rootCaBytes) || Arrays.equals(caBytes, rootDebugCaBytes)) {
      return true;
    }
    return false;
  }

}

