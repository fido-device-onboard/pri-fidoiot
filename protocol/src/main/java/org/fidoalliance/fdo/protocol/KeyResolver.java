package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.KeyStoreInputStreamFunction;
import org.fidoalliance.fdo.protocol.dispatch.KeyStoreOutputStreamFunction;
import org.fidoalliance.fdo.protocol.dispatch.ValidityDaysSupplier;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

public class KeyResolver {

  //Java Algorithms names for Signatures
  protected KeyStoreConfig config;
  protected KeyStore keyStore;

  public KeyResolver() {
  }

  protected char[] getPasswordArray() {
    String pass = config.getPassword();
    if (pass != null) {
      return pass.toCharArray();
    }
    return "".toCharArray();
  }

  private void generateKey(PublicKeyType keyType,
      KeySizeType sizeType)
      throws IOException {
    CryptoService cs = Config.getWorker(CryptoService.class);
    KeyPair keyPair = cs.createKeyPair(keyType, sizeType);

    try {

      final String sigAlgorithm =
          new AlgorithmFinder().getSignatureAlgorithm(keyType,sizeType);

      Certificate[] chain = new CertChainBuilder()
          .setPrivateKey(keyPair.getPrivate())
          .setPublicKey(keyPair.getPublic())
          .setProvider(cs.getProvider())
          .setSignatureAlgorithm(sigAlgorithm)
          .setSubject("CN=FdoEntity")
          .setValidityDays(Config.getWorker(ValidityDaysSupplier.class).get())
          .build();

      keyStore.setKeyEntry(KeyResolver.getAlias(keyType,sizeType),
          keyPair.getPrivate(),
          getPasswordArray(),
          chain);

    } catch (KeyStoreException e) {
      throw new IOException(e);
    }
    finally {
      cs.destroyKey(keyPair);
    }
  }

  public void load(KeyStoreConfig config) throws IOException {
    try {
      this.config = config;
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

  public void store() throws IOException{
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

  public Certificate[] getCertificateChain(String alias) throws IOException {
    try {
      return keyStore.getCertificateChain(alias);
    } catch (KeyStoreException e) {
      throw new IOException(e);
    }
  }

  public PrivateKey getPrivateKey(PublicKey publicKey) throws IOException {
    try {
      Enumeration<String> aliases = keyStore.aliases();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        if (keyStore.getCertificate(alias).getPublicKey().equals(publicKey)) {
          return getPrivateKey(alias);
        }
      }
      throw new IOException(new NoSuchElementException("private key for public key"));
    } catch (KeyStoreException e) {
      throw new IOException(e);
    }
  }

  public PrivateKey getPrivateKey(String alias) throws IOException {
    try {
      return (PrivateKey) keyStore.getKey(alias,getPasswordArray());
    } catch (KeyStoreException e) {
      throw new IOException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    } catch (UnrecoverableKeyException e) {
      throw new IOException(e);
    }
  }

  public static String getAlias(PublicKeyType keyType, KeySizeType keySize) {
    if (keyType.equals(PublicKeyType.RSAPKCS)) {
      return keyType.name() + Integer.toString(keySize.toInteger());
    }
    return keyType.name();
  }




}
