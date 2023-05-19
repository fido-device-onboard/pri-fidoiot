// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.KeyStoreInputStreamFunction;
import org.fidoalliance.fdo.protocol.dispatch.KeyStoreOutputStreamFunction;
import org.fidoalliance.fdo.protocol.dispatch.ValidityDaysSupplier;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;
import org.fidoalliance.fdo.protocol.message.SigInfoType;

/**
 * Resolves keys.
 */
public class KeyResolver {

  //Java Algorithms names for Signatures
  protected KeyStoreConfig config;
  protected KeyStore keyStore;

  /**
   * Default Constructor.
   */
  public KeyResolver() {
  }

  /**
   * Gets the password to decrypt the key.
   *
   * @return The password of the keystore
   */
  protected char[] getPasswordArray() {
    String pass = config.getPassword();
    if (pass != null) {
      return pass.toCharArray();
    }
    return "".toCharArray();
  }

  /**
   * Generates a key.
   *
   * @param keyType  The key type.
   * @param sizeType The size of the key.
   * @throws IOException An error occurred.
   */
  private void generateKey(PublicKeyType keyType,
      KeySizeType sizeType)
      throws IOException {
    CryptoService cs = Config.getWorker(CryptoService.class);
    KeyPair keyPair = cs.createKeyPair(keyType, sizeType);

    try {

      final String sigAlgorithm =
          new AlgorithmFinder().getSignatureAlgorithm(keyType, sizeType);

      Certificate[] chain = new CertChainBuilder()
          .setPrivateKey(keyPair.getPrivate())
          .setPublicKey(keyPair.getPublic())
          .setProvider(cs.getProvider())
          .setSignatureAlgorithm(sigAlgorithm)
          .setSubject("CN=FdoEntity CA")
          .setValidityDays(Config.getWorker(ValidityDaysSupplier.class).get())
          .setCA(true)
          .build();

      keyStore.setKeyEntry(KeyResolver.getAlias(keyType, sizeType),
          keyPair.getPrivate(),
          getPasswordArray(),
          chain);

    } catch (KeyStoreException e) {
      throw new IOException(e);
    } finally {
      cs.destroyKey(keyPair);
    }
  }

  /**
   * Gets the underlying keystore.
   *
   * @return The underlying keystore.
   */
  public KeyStore getKeyStore() {
    return this.keyStore;
  }

  /**
   * Loads the keys to be resolved.
   *
   * @param config The Keystore description.
   * @throws IOException An error occured.
   */
  public void load(KeyStoreConfig config) throws IOException {
    this.config = config;
    if (config.getStoreType().equals("PEM")) {

      //load from pem

      try {
        String pemString = Files.readString(Path.of(config.getPath()));
        List<Certificate> certs = PemLoader.loadCerts(pemString);
        final String keyPass = config.getPassword();
        PrivateKey privateKey = PemLoader.loadPrivateKey(pemString, keyPass);

        this.keyStore = KeyStore.getInstance("JKS");
        this.keyStore.load(null, getPasswordArray());

        this.keyStore.setKeyEntry(config.getAlias(), privateKey, getPasswordArray(),
            certs.stream().toArray(Certificate[]::new));


      } catch (KeyStoreException e) {
        throw new IOException(e);
      } catch (CertificateException e) {
        throw new IOException(e);
      } catch (NoSuchAlgorithmException e) {
        throw new IOException(e);
      }
    } else {
      try {
        this.keyStore = KeyStore.getInstance(config.getStoreType());

        String path = config.getPath();
        if (path != null) { // we have a stream path to load from
          try (InputStream input =
              Config.getWorker(KeyStoreInputStreamFunction.class).apply(path)) {
            keyStore.load(input, getPasswordArray());
          }
          buildKeyStore();
        } else {
          //assumed to be PKSC11/HSM store
          keyStore.load(null, getPasswordArray());
        }
      } catch (KeyStoreException e) {
        throw new IOException(e);
      } catch (CertificateException e) {
        throw new IOException(e);
      } catch (NoSuchAlgorithmException e) {
        throw new IOException(e);
      }
    }
  }

  /**
   * Stores the keys.
   *
   * @throws IOException An error occurred.
   */
  public void store() throws IOException {
    if (keyStore == null || config == null) {
      throw new IOException(new IllegalStateException("key store not loaded"));
    }

    try {
      String path = config.getPath();
      if (path != null) { // we have a stream path to load from
        try (OutputStream out =
            Config.getWorker(KeyStoreOutputStreamFunction.class).apply(path)) {
          keyStore.store(out, getPasswordArray());
        }
      } else {
        //assumed to be PKSC11/HSM store
        keyStore.store(null, getPasswordArray());
      }
    } catch (KeyStoreException e) {
      throw new IOException(e);
    } catch (IOException e) {
      throw new IOException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    } catch (CertificateException e) {
      throw new IOException(e);
    }
  }

  /**
   * Builds the keystore with all spec key types and sizes.
   *
   * @throws IOException An error occurred.
   */
  protected void buildKeyStore() throws IOException {

    if (keyStore == null || config == null) {
      throw new IOException(new IllegalStateException("key store not loaded"));
    }
    try {
      if (!keyStore.aliases().hasMoreElements()) {
        if (config.getPath() != null) {
          generateKey(PublicKeyType.RSA2048RESTR, KeySizeType.SIZE_2048);
          generateKey(PublicKeyType.RSAPKCS, KeySizeType.SIZE_2048);
          generateKey(PublicKeyType.RSAPKCS, KeySizeType.SIZE_3072);
          generateKey(PublicKeyType.SECP256R1, KeySizeType.SIZE_256);
          generateKey(PublicKeyType.SECP384R1, KeySizeType.SIZE_384);
          store();
        }
      }
    } catch (KeyStoreException e) {
      throw new IOException(e);
    }
  }

  /**
   * Gets the certificate chain of an alias.
   *
   * @param alias The alias name.
   * @return The certificate chain.
   * @throws IOException An error occurred.
   */
  public Certificate[] getCertificateChain(String alias) throws IOException {
    try {
      return keyStore.getCertificateChain(alias);
    } catch (KeyStoreException e) {
      throw new IOException(e);
    }
  }

  /**
   * Get the certificate chain of the first key in the store.
   *
   * @return The certificate chain of the first key.
   * @throws IOException An error occurred.
   */
  public Certificate[] getCertificateChain() throws IOException {
    try {
      Enumeration<String> aliases = keyStore.aliases();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        return getCertificateChain(alias);
      }
      throw new IOException(new NoSuchElementException("private key for public key"));
    } catch (KeyStoreException e) {
      throw new IOException(e);
    }
  }

  /**
   * Resolves a public key to its associated private key.
   *
   * @param publicKey A public key.
   * @return The private key associated with the public key.
   * @throws IOException An error occurred.
   */
  public PrivateKey getPrivateKey(PublicKey publicKey) throws IOException {
    try {
      Enumeration<String> aliases = keyStore.aliases();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        PublicKey findKey = keyStore.getCertificate(alias).getPublicKey();
        if (findKey.equals(publicKey)) {
          return getPrivateKey(alias);
        }
      }
      throw new IOException(new NoSuchElementException("private key for public key"));
    } catch (KeyStoreException e) {
      throw new IOException(e);
    }
  }


  /**
   * Gets the private key from a given alias.
   *
   * @param alias The alias name.
   * @return The private key.
   * @throws IOException An error occurred.
   */
  public PrivateKey getPrivateKey(String alias) throws IOException {
    try {
      return (PrivateKey) keyStore.getKey(alias, getPasswordArray());
    } catch (KeyStoreException e) {
      throw new IOException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    } catch (UnrecoverableKeyException e) {
      throw new IOException(e);
    }
  }

  /**
   * Limits the resolver to one alias.
   *
   * @param defAlias The alias to keep in the store.
   * @throws IOException An error occurred.
   */
  public void setAlias(String defAlias) throws IOException {
    try {
      Enumeration<String> aliases = keyStore.aliases();
      List<String> removeList = new ArrayList<>();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        if (alias.compareToIgnoreCase(defAlias) != 0) {
          removeList.add(alias);
        }
      }
      for (String alias : removeList) {
        keyStore.deleteEntry(alias);
      }
    } catch (KeyStoreException e) {
      throw new IOException(e);
    }
  }

  /**
   * Gets the alias for a given key type and size.
   *
   * @param keyType The key type.
   * @param keySize the key size.
   * @return The alias name for the key.
   */
  public static String getAlias(PublicKeyType keyType, KeySizeType keySize) {
    if (keyType.equals(PublicKeyType.RSAPKCS)) {
      return keyType.name() + keySize.toInteger();
    }
    return keyType.name();
  }

  /**
   * Gets the alias from the given signature info.
   *
   * @param sigInfoType The sigInfo Type.
   * @return The Alias
   * @throws IOException An error occurred.
   */
  public static String getAlias(SigInfoType sigInfoType) throws IOException {
    switch (sigInfoType) {
      case SECP256R1:
        return PublicKeyType.SECP256R1.name();
      case SECP384R1:
        return PublicKeyType.SECP384R1.name();
      default:
        throw new InternalServerErrorException(new IllegalArgumentException());
    }
  }


}
