package org.fidoalliance.fdo.protocol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class PemKeyStoreImpl extends KeyStoreSpi {

  private static final class PemEntry {

    public PrivateKey key;
    public Certificate cert;
    public Certificate[] chain;
  }

  private final Map<String, PemEntry> entryMap = new HashMap<>();
  private final Date date = new Date(System.currentTimeMillis());


  @Override
  public Key engineGetKey(String alias, char[] password)
      throws NoSuchAlgorithmException, UnrecoverableKeyException {
    if (entryMap.containsKey(alias)) {
      return entryMap.get(alias).key;
    }
    return null;
  }

  @Override
  public Certificate[] engineGetCertificateChain(String alias) {

    if (entryMap.containsKey(alias)) {
      return entryMap.get(alias).chain;
    }
    return null;
  }

  @Override
  public Certificate engineGetCertificate(String alias) {

    if (entryMap.containsKey(alias)) {
      return entryMap.get(alias).cert;
    }
    return null;
  }

  @Override
  public Date engineGetCreationDate(String alias) {
    return date;
  }

  @Override
  public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain)
      throws KeyStoreException {

    PemEntry entry = entryMap.get(alias);
    if (entry == null) {
      entry = new PemEntry();
      entryMap.put(alias,entry);
    }
    entry.key = (PrivateKey) key;
    entry.chain = chain;
    if (chain != null && chain.length > 0) {
      entry.cert = chain[0];
    }
  }

  @Override
  public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
      throws KeyStoreException {

    throw new KeyStoreException(new UnsupportedOperationException());
  }

  @Override
  public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {

    throw new KeyStoreException(new UnsupportedOperationException());
  }

  @Override
  public void engineDeleteEntry(String alias) throws KeyStoreException {

    throw new KeyStoreException(new UnsupportedOperationException());
  }

  @Override
  public Enumeration<String> engineAliases() {
    return Collections.enumeration(entryMap.keySet());
  }

  @Override
  public boolean engineContainsAlias(String alias) {
    return entryMap.containsKey(alias);
  }

  @Override
  public int engineSize() {
    return 0;
  }

  @Override
  public boolean engineIsKeyEntry(String alias) {

    if (entryMap.containsKey(alias)) {
      return entryMap.get(alias).key != null;
    }
    return false;
  }

  @Override
  public boolean engineIsCertificateEntry(String alias) {
    if (entryMap.containsKey(alias)) {
      PemEntry entry =  entryMap.get(alias);
      return entry.key == null && (entry.chain != null || entry.cert != null);
    }
    return false;
  }

  @Override
  public String engineGetCertificateAlias(Certificate cert) {
    for (Map.Entry<String, PemEntry> entry : entryMap.entrySet()) {
      if (entry.getValue().cert != null && entry.getValue().cert.equals(cert)) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public void engineStore(OutputStream stream, char[] password)
      throws IOException, NoSuchAlgorithmException, CertificateException {

    throw new IOException(new UnsupportedOperationException());
  }

  @Override
  public void engineLoad(InputStream stream, char[] password)
      throws IOException, NoSuchAlgorithmException, CertificateException {

    if (stream != null) {

      final String pemString = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      String passString = null;
      if (password != null) {
        passString = new String(password);
      }
      final PrivateKey key = PemLoader.loadPrivateKey(pemString, passString);
      final List<Certificate> certs = PemLoader.loadCerts(pemString);
      if (key != null && certs != null) {
        PemEntry entry = new PemEntry();
        entry.key = key;
        entry.chain = certs.stream().toArray(Certificate[]::new);
        if (entry.chain.length > 0) {
          entry.cert = entry.chain[0];
        }
        entryMap.put(Integer.toString(0), entry);
      } else if (certs != null) {
        int i = 0;
        for (Certificate cert : certs) {
          PemEntry entry = new PemEntry();
          entry.cert = cert;
          entryMap.put(Integer.toString(i++), entry);
        }

      } else {
        try (Scanner scanner = new Scanner(pemString)) {
          int i = 0;
          while (scanner.hasNextLine()) {
            final String line = scanner.nextLine();
            final File file = new File(line);
            if (file.exists()) {
              final List<Certificate> dirCerts = PemLoader.loadCerts(pemString);
              final PrivateKey dirKey = PemLoader.loadPrivateKey(pemString, passString);
              if (dirKey != null && dirCerts != null) {
                PemEntry entry = new PemEntry();
                entry.key = dirKey;

                entry.chain = dirCerts.stream().toArray(Certificate[]::new);
                if (entry.chain.length > 0) {
                  entry.cert = entry.chain[0];
                }
                entryMap.put(Integer.toString(i++), entry);
              }
            }
          }
        }
      }
    }
  }
}
