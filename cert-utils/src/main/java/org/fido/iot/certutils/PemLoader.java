// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.certutils;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * Loads X509Certificates, Public keys, and Private keys from PEM formatted strings.
 */
public class PemLoader {

  /**
   * Loads the X509Certificate chain from a string in PEM format.
   * @param pemString  A String containing PEM information.
   * @return A List of X509 Certificates found in the PEM string.
   */
  public static List<Certificate> loadCerts(String pemString) {
    List<Certificate> certs = new ArrayList<>();
    try {
      PEMParser parser = new PEMParser(new StringReader(pemString));
      JcaX509CertificateConverter jcac = new JcaX509CertificateConverter();
      for (; ; ) {
        Object obj = parser.readObject();
        if (obj == null) {
          break;
        }
        if (obj instanceof X509CertificateHolder) {
          X509CertificateHolder holder = (X509CertificateHolder) obj;
          certs.add(jcac.getCertificate(holder));
        }
      }
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return certs;
  }

  /**
   * Loads a private key from a PEM formatted String.
   * @param pemString A String containing PEM information.
   * @return The PrivateKey found in the PEM String.
   */
  public static PrivateKey loadPrivateKey(String pemString) {
    try {
      PEMParser parser = new PEMParser(new StringReader(pemString));
      for (; ; ) {
        Object obj = parser.readObject();
        if (obj == null) {
          break;
        }
        if (obj instanceof PEMKeyPair) {
          PEMKeyPair kp = (PEMKeyPair) obj;
          return new JcaPEMKeyConverter().getPrivateKey(kp.getPrivateKeyInfo());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException(new CertStoreException());
  }

  /**
   * Loads all the public keys found in a PEM string.
   * @param pemString A String containing PEM information.
   * @return  The list of all public Keys found in the PEM file.
   */
  public static List<PublicKey> loadPublicKeys(String pemString) {
    List<PublicKey> keys = new ArrayList<>();
    try {
      PEMParser parser = new PEMParser(new StringReader(pemString));
      JcaPEMKeyConverter jcac = new JcaPEMKeyConverter();
      for (; ; ) {
        Object obj = parser.readObject();
        if (obj == null) {
          break;
        }
        if (obj instanceof SubjectPublicKeyInfo) {
          keys.add(jcac.getPublicKey((SubjectPublicKeyInfo) obj));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return keys;
  }
}
