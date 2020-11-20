// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A keystore implementation of a key resolver.
 */
public abstract class KeyStoreResolver implements KeyResolver {

  private KeyStore keyStore;

  protected abstract String getPassword();

  protected abstract String getKeyStoreType();

  protected abstract String getKeyStorePath();

  /**
   * Gets the keystore associated with the resolver.
   *
   * @return The underlying keystore.
   */
  public KeyStore getKeyStore() {
    if (keyStore == null) {
      try {
        String pwd = getPassword();
        String storeType = getKeyStoreType();
        keyStore = KeyStore.getInstance(storeType);

        InputStream input = null;

        String path = getKeyStorePath();
        if (path != null && path.length() > 0) {
          input = new FileInputStream(new File(path));
        }
        keyStore.load(input, pwd.toCharArray());
      } catch (KeyStoreException e) {
        throw new RuntimeException(e);
      } catch (CertificateException e) {
        throw new RuntimeException(e);
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return keyStore;
  }

  @Override
  public PrivateKey getKey(PublicKey key) {
    String pwd = getPassword();
    try {
      Iterator<String> aliases = getKeyStore().aliases().asIterator();
      while (aliases.hasNext()) {
        String alias = aliases.next();
        Certificate certificate = getKeyStore().getCertificate(alias);
        if (null == certificate) {
          continue;
        }
        if (Arrays.equals(certificate.getPublicKey().getEncoded(), key.getEncoded())) {
          return (PrivateKey) getKeyStore().getKey(alias, pwd.toCharArray());
        }
      }
    } catch (UnrecoverableKeyException e) {
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (KeyStoreException e) {
      throw new RuntimeException(e);
    }
    return null;
  }
}
