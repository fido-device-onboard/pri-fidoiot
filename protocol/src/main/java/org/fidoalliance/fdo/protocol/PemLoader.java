// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 * Loads X509Certificates, Public keys, and Private keys from PEM formatted strings.
 */
public class PemLoader {


  /**
   * Load tagged Certificates.
   *
   * @param pemString A PEM string.
   * @return The certificate chain.
   * @throws IOException An Error occurred.
   */
  public static List<Certificate> loadTaggedCerts(String pemString) throws IOException {
    List<Certificate> certs = new ArrayList<>();
    StringReader reader = new StringReader(pemString);
    PemReader pemReader = new PemReader(reader);
    try {
      while (true) {
        PemObject o = pemReader.readPemObject();
        if (o == null) {
          break;
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bis = new ByteArrayInputStream(o.getContent());
        Certificate cert = cf.generateCertificate(bis);
        certs.add(cert);
      }
    } catch (IOException | CertificateException e) {
      throw new IOException(e);
    }
    return certs;
  }

  /**
   * Loads the X509Certificate chain from a string in PEM format.
   *
   * @param pemString A String containing PEM information.
   * @return A List of X509 Certificates found in the PEM string.
   */
  public static List<Certificate> loadCerts(String pemString) {
    List<Certificate> certs = new ArrayList<>();
    try {
      PEMParser parser = new PEMParser(new StringReader(pemString));
      for (; ; ) {
        Object obj = parser.readPemObject();
        if (obj == null) {
          break;
        }
        PemObject pemObj = (PemObject) obj;
        if (pemObj.getType().equals("CERTIFICATE")) {
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          Certificate cert = cf.generateCertificate(
              new ByteArrayInputStream(((PemObject) obj).getContent()));

          certs.add(cert);
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
   *
   * @param pemString A String containing PEM information.
   * @param password  A Pem password or null.
   * @return The PrivateKey found in the PEM String.
   */
  public static PrivateKey loadPrivateKey(String pemString, String password) {
    try {
      PEMParser parser = new PEMParser(new StringReader(pemString));
      for (; ; ) {
        Object obj = parser.readObject();
        if (obj == null) {
          break;
        }

        if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {

          PKCS8EncryptedPrivateKeyInfo epki = (PKCS8EncryptedPrivateKeyInfo) obj;

          JcePKCSPBEInputDecryptorProviderBuilder builder =
              new JcePKCSPBEInputDecryptorProviderBuilder()
                      .setProvider(new BouncyCastleFipsProvider());

          InputDecryptorProvider idp = null;
          if (password != null) {
            idp = builder.build(password.toCharArray());
          } else {
            idp = builder.build(null);
          }

          PrivateKeyInfo pki = epki.decryptPrivateKeyInfo(idp);
          return new JcaPEMKeyConverter().getPrivateKey(pki);
        } else if (obj instanceof PEMKeyPair) {

          PEMKeyPair kp = (PEMKeyPair) obj;
          return new JcaPEMKeyConverter().getPrivateKey(kp.getPrivateKeyInfo());

        } else if (obj instanceof PrivateKeyInfo) {
          return new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) obj);
        }
      }
    } catch (IOException | PKCSException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  /**
   * Loads all the public keys found in a PEM string.
   *
   * @param pemString A String containing PEM information.
   * @return The list of all public Keys found in the PEM file.
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

        } else if (obj instanceof X509CertificateHolder) {
          keys.add(jcac.getPublicKey(((X509CertificateHolder) obj).getSubjectPublicKeyInfo()));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return keys;
  }
}
