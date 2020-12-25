// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.ondie;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.fido.iot.protocol.Const;


public class OnDieService {

  private boolean checkRevocations = true;

  private final List<URL> sourceUrlList = new ArrayList<URL>();

  private HashMap<String, byte[]> cacheMap = new HashMap<String, byte[]>();

  private final String cacheUpdatedTouchFile = "cache_updated";

  private OnDieCache onDieCache;

  static final int taskInfoLength = 36;  // length of the taskinfo part of OnDie signature
  static final int rLength = 48;  // length of the r field part of OnDie signature
  static final int sLength = 48;  // length of the s field part of OnDie signature

  /**
   * Constructor.
   *
   * @param onDieCache onDieCache
   * @param checkRevocations checkRevocations
   */
  public OnDieService(OnDieCache onDieCache,
                      final boolean checkRevocations) {
    this.onDieCache = onDieCache;
    this.checkRevocations = checkRevocations;
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
   * @param checkRevocations checkRevocations
   * @return boolean indicating if signature is valid.
   */
  public boolean validateSignature(List<Certificate> certChain,
                                   byte[] signedData,
                                   byte[] signature,
                                   boolean checkRevocations) {

    // Check revocations first.
    if ((certChain == null) || (checkRevocations && !checkRevocations(certChain))) {
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
              byte[] crlBytes = onDieCache.getCertOrCrl(generalName.toString());
              if (crlBytes == null) {
                /* TODO LoggerFactory.getLogger(getClass()).error(
                    "CRL ({}) not found in cache for cert: {}",
                    generalName.getName().toString(),
                    x509cert.getIssuerX500Principal().getName());*/
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


}

