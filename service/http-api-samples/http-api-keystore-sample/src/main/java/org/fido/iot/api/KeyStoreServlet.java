// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;
import java.util.UUID;
import javax.security.auth.DestroyFailedException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fido.iot.certutils.PemLoader;

/**
 * Keystore management API servlet.
 */
public class KeyStoreServlet extends HttpServlet {

  public static final String STORE_ATTRIBUTE = "keystore";
  public static final String STORE_PASSWORD = "keystore_password";

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String alias = req.getParameter("alias");
    if (null == alias) {
      resp.setStatus(400);
      return;
    }

    KeyStore keyStore = (KeyStore) getServletContext().getAttribute(STORE_ATTRIBUTE);

    try {
      keyStore.deleteEntry(alias);
    } catch (KeyStoreException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    KeyStore keyStore = (KeyStore) getServletContext().getAttribute(STORE_ATTRIBUTE);
    String pwd = getServletContext().getAttribute(STORE_PASSWORD).toString();

    String alias = req.getParameter("alias");
    if (alias == null) {
      alias = UUID.randomUUID().toString();
    }

    String pemString = new String(req.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);

    List<Certificate> certs = PemLoader.loadCerts(pemString);

    if (certs.size() == 0) {
      resp.setStatus(400);
      return;
    }

    Certificate[] certArray = new Certificate[certs.size()];
    certArray = certs.toArray(certArray);

    try {
      PrivateKey key = PemLoader.loadPrivateKey(pemString);
      keyStore.setKeyEntry(alias, key, pwd.toCharArray(), certArray);

      try {
        key.destroy();
      } catch (DestroyFailedException e) {
        //suppress error if not support
      }
    } catch (KeyStoreException e) {
      throw new ServletException(e);
    }
  }
}
