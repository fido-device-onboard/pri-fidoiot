// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Iterator;

import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.CloseableKey;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.storage.CertificateResolver;

/**
 * AIO Manufacturing certificate resolver.
 */
public class AioCertificateResolver implements CertificateResolver {

  private KeyStore mfgKeyStore;
  private final String mfgKeystorePath;
  private final String mfgKeyStorePin;
  private final CryptoService cs;
  private static final LoggerService logger = new LoggerService(AioCertificateResolver.class);

  /**
   * Constructs an instance of a AioCertificateResolver.
   * @param cs An instance to a CryptoService.
   * @param storePath The path to the certificate store (can be empty if PKCS11).
   * @param storeType The store type (e.g. PKCS12 or PKCS11)
   * @param storePin The store pin/password
   */
  public AioCertificateResolver(CryptoService cs,
      String storePath, String storeType, String storePin) {

    this.mfgKeystorePath = storePath;
    this.mfgKeyStorePin = storePin;
    this.cs = cs;

    try {
      if (null == mfgKeyStorePin) {
        throw new IOException();
      }
      mfgKeyStore = KeyStore.getInstance(storeType);
      if (storePath != null && storePath.length() > 0) {
        try (FileInputStream inFile = new FileInputStream(mfgKeystorePath)) {
          mfgKeyStore.load(inFile, mfgKeyStorePin.toCharArray());
        }
      } else {
        mfgKeyStore.load(null, mfgKeyStorePin.toCharArray());
      }

    } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public CloseableKey getPrivateKey(Certificate cert) {
    if (null != mfgKeyStore && null != cert) {
      try {
        Iterator<String> aliases = mfgKeyStore.aliases().asIterator();
        while (aliases.hasNext()) {
          String alias = aliases.next();
          Certificate certificate = mfgKeyStore.getCertificate(alias);
          if (null == certificate) {
            continue;
          }
          if (Arrays.equals(certificate.getEncoded(), cert.getEncoded())) {
            return new CloseableKey((PrivateKey) mfgKeyStore.getKey(alias,
                mfgKeyStorePin.toCharArray()));
          }
        }
      } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException
          | CertificateEncodingException e) {
        logger.warn("Unable to retrieve Private Key. " + e.getMessage());
      }
    }
    throw new RuntimeException();
  }

  @Override
  public Certificate[] getCertChain(int publicKeyType) {
    String algName;
    switch (publicKeyType) {
      case Const.PK_RSA2048RESTR:
      case Const.PK_RSA3072:
        algName = Const.RSA_ALG_NAME;
        break;
      case Const.PK_SECP256R1:
      case Const.PK_SECP384R1:
        algName = Const.EC_ALG_NAME;
        break;
      default:
        throw new RuntimeException();
    }
    if (null != mfgKeyStore) {
      try {
        Iterator<String> aliases = mfgKeyStore.aliases().asIterator();
        while (aliases.hasNext()) {
          String alias = aliases.next();
          Certificate[] certificateChain = mfgKeyStore.getCertificateChain(alias);
          if (certificateChain != null && certificateChain.length > 0
              && certificateChain[0].getPublicKey().getAlgorithm().equals(algName)
              && publicKeyType == cs.getPublicKeyType(certificateChain[0].getPublicKey())) {
            return certificateChain;
          }
        }
      } catch (KeyStoreException e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException();
  }
}
