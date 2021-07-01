// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
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
import org.fidoalliance.fdo.protocol.KeyResolver;

public class AioOwnerKeyResolver implements KeyResolver {

  private static KeyStore ownerKeyStore;
  private final String ownerKeystorePath;
  private final String ownerKeyStoreType;
  private final String ownerKeyStorePin;

  /**
   * Constructor.
   */
  public AioOwnerKeyResolver(String ownerKeystorePath,
      String ownerKeyStoreType,
      String ownerKeyStorePin) {
    this.ownerKeystorePath = ownerKeystorePath;
    this.ownerKeyStoreType = ownerKeyStoreType;
    this.ownerKeyStorePin = ownerKeyStorePin;
    initOwnerKeystore();
  }

  @Override
  public PrivateKey getKey(PublicKey key) {
    if (null != ownerKeyStore && null != key) {
      try {
        Iterator<String> aliases = ownerKeyStore.aliases().asIterator();
        while (aliases.hasNext()) {
          String alias = aliases.next();
          Certificate certificate = ownerKeyStore.getCertificate(alias);
          if (null == certificate) {
            continue;
          }
          if (Arrays.equals(certificate.getPublicKey().getEncoded(), key.getEncoded())) {
            return (PrivateKey) ownerKeyStore.getKey(alias, ownerKeyStorePin.toCharArray());
          }
        }
      } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
        System.out.println(e.getMessage());
      }
    }
    return null;
  }

  private void initOwnerKeystore() {
    try {
      if (null == ownerKeystorePath || null == ownerKeyStorePin) {
        throw new IOException();
      }
      if (null == ownerKeyStore) {
        ownerKeyStore = KeyStore.getInstance(ownerKeyStoreType);
        Path keystorePath = Path.of(ownerKeystorePath);
        if (!keystorePath.toAbsolutePath().toFile().exists()) {
          throw new IOException();
        }
        ownerKeyStore.load(new FileInputStream(keystorePath.toAbsolutePath().toFile()),
            ownerKeyStorePin.toCharArray());
      }
    } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
      System.out.println("Keystore not configured.");
      System.out.println(e.getMessage());
    }
  }


}
