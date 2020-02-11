// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.NoSuchProviderException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CertPathFactory {

  private static final Logger LOG = LoggerFactory.getLogger(CertPathFactory.class);
  private static final String X_509 = "X.509";
  private final URI uri;

  private CertPathFactory(URI uri) {
    this.uri = uri;
  }

  /**
   * Return a new CertPath object.
   */
  public CertPath getObject()
      throws CertificateException, MalformedURLException, NoSuchProviderException {

    if (null != uri) {
      URL url = uri.toURL();
      List<Certificate> certs = new ArrayList<>();

      try (InputStream in = url.openStream();
          Reader reader = new BufferedReader(new InputStreamReader(in))) {

        JcaX509CertificateConverter cc = new JcaX509CertificateConverter();
        cc.setProvider(BouncyCastleLoader.load());

        try (PEMParser pem = new PEMParser(reader)) {

          for (Object o = pem.readObject(); null != o; o = pem.readObject()) {

            if (o instanceof X509CertificateHolder) {
              X509CertificateHolder holder = (X509CertificateHolder) o;
              certs.add(cc.getCertificate(holder));
            }
          }
        }

      } catch (IOException e) {
        ; // if we couldn't open the URL, treat it as not-set
        LOG.debug(e.getMessage(), e);
      }

      CertificateFactory factory = CertificateFactory.getInstance(X_509, BouncyCastleLoader.load());
      return factory.generateCertPath(certs);

    } else {
      return null;
    }
  }
}
