// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.File;
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
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.KeyResolver;

public class OwnerKeyResolver implements KeyResolver {

  private static KeyStore ownerKeyStore;
  private final String ownerKeystorePath;
  private final String ownerKeyStorePin;
  private static LoggerService logger;
  
  /**
   * Constructor.
   */
  public OwnerKeyResolver(String ownerKeystorePath, String ownerKeyStorePin) {
    this.ownerKeystorePath = ownerKeystorePath;
    this.ownerKeyStorePin = ownerKeyStorePin;
    logger = new LoggerService(OwnerKeyResolver.class);
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
        logger.error("Failed to get private key for given public key.");
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
        ownerKeyStore = KeyStore.getInstance(OwnerAppSettings.OWNER_KEYSTORE_TYPE);
        Path keystorePath = Path.of(ownerKeystorePath);
        if (!keystorePath.toAbsolutePath().toFile().exists()) {
          throw new IOException();
        }
        ownerKeyStore.load(new FileInputStream(keystorePath.toAbsolutePath().toFile()),
            ownerKeyStorePin.toCharArray());
      }
    } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
      logger.error("Keystore is not properly configured.");
    }
  }
}
