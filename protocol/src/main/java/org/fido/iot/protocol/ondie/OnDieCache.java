// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol.ondie;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class OnDieCache {

  private boolean autoUpdate = false;
  private String cacheDir = "";
  private boolean initialized = false;

  List<String> rootCaList = new ArrayList<String>(Arrays.asList(
          "OnDie_CA_RootCA_Certificate.cer",
          "OnDie_CA_DEBUG_RootCA_Certificate.cer"));

  // set to default and override in constructor if user wishes
  private String artifactZipUrl = "https://tsci.intel.com/content/csme.zip";

  private HashMap<String, byte[]> cacheMap = new HashMap<String, byte[]>();

  private final String cacheUpdatedTouchFile = "cache_updated";

  /**
   * Constructor.
   *
   * @param cacheDir cacheDir
   * @param autoUpdate autoUpdate
   * @param artifactZipUrl URL of zip file containing certs and crls
   *                            (only used if autoUpdate is set to true)
   */
  public OnDieCache(final String cacheDir,
                    final boolean autoUpdate,
                    final String artifactZipUrl,
                    final List<String> rootCaCerts) {
    this.cacheDir = cacheDir;
    this.autoUpdate = autoUpdate;

    if (artifactZipUrl != null && !artifactZipUrl.isEmpty()) {
      this.artifactZipUrl = artifactZipUrl;
    }

    if (rootCaCerts != null && !rootCaCerts.isEmpty()) {
      rootCaList = rootCaCerts;
    }
  }

  /**
   * Initialization.
   *
   */
  public void initializeCache() throws IOException {

    if (initialized) {
      return;
    }

    if (cacheDir != null) {
      File cache = new File(cacheDir);
      if (!cache.exists()) {
        throw new IOException("OnDieCertCache: cache directory does not exist: " + cacheDir);
      }
      if (!cache.isDirectory()) {
        throw new IOException("OnDieCertCache: cache directory must be a directory: " + cacheDir);
      }

      this.cacheDir = cacheDir;
      this.autoUpdate = autoUpdate;
      if (autoUpdate) {
        // update local cache
        copyFromUrlSources();
      }
      loadCacheMap();
    }
    initialized = true;
  }


  /**
   * Copy the certs and CRLs from the URL source to the cache directory.
   *
   */
  private void copyFromUrlSources() throws IOException {

    File cache = new File(cacheDir);
    if (!cache.exists()) {
      cache.mkdir();
    }

    URL url = new URL(artifactZipUrl);
    URLConnection urlConn = url.openConnection();
    try (InputStream zipFileInput = urlConn.getInputStream()) {
      ZipInputStream zipInput = new ZipInputStream(zipFileInput);
      ZipEntry zipEntry = zipInput.getNextEntry();
      while (zipEntry != null) {
        System.out.println(zipEntry.getName());
        if (zipEntry.getName().startsWith("content/OnDieCA")
            && (zipEntry.getName().endsWith(".cer") || zipEntry.getName().endsWith(".crl"))) {
          Path p = Paths.get(zipEntry.getName());
          File newFile = new File(cacheDir, p.getFileName().toString());
          FileOutputStream fos = new FileOutputStream(newFile);
          byte[] buffer = new byte[1024];
          int len;
          while ((len = zipInput.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
          fos.close();
        }
        zipEntry = zipInput.getNextEntry();
      }
    }
  }

  /**
   * Loads the memory map of cache values from the cache directory.
   *
   * @throws Exception if error
   */
  private void loadCacheMap() throws IOException {
    if (cacheDir != null) {
      File cache = new File(cacheDir);

      // Read each file and load into the hashmap
      File[] files = new File(cache.getAbsolutePath()).listFiles();
      if (files != null) {
        for (File file : files) {
          if (!file.isDirectory()) {
            if (file.getName().toLowerCase().endsWith(".crl")
                || file.getName().toLowerCase().endsWith(".cer")) {
              cacheMap.put(file.getName(), Files.readAllBytes(Paths.get(file.getAbsolutePath())));
            }
          }
        }
      }
    }
  }


  private boolean isCacheUpdateNeeded() throws IOException {
    if (cacheDir != null) {
      File cache = new File(cacheDir);

      // check for the "files updated" touch file
      if (Files.exists(Paths.get(cache.getAbsolutePath(), cacheUpdatedTouchFile))) {
        // rename all .new files and remove touch file
        FilenameFilter filter = (dir, name) -> name.endsWith(".new");
        File[] files = new File(cache.getAbsolutePath()).listFiles(filter);
        for (File file : files) {
          File targetFile = new File(file.getAbsolutePath().replaceAll(".new", ""));
          targetFile.delete();
          file.renameTo(targetFile);
          file.delete();
        }
        Files.delete(Paths.get(cacheDir, cacheUpdatedTouchFile));
        return true;
      }
    }
    return false;
  }


  /**
   * Returns the certificate or CRL corresponding to the specified pathname.
   * The pathname is the full original pathname to the cert file or CRL file.
   * The return value is the byte[] or the cert.
   *
   * @param pathName pathName of cache entry to retrieve
   * @return byte[] cert or crl bytes
   * @throws Exception if error
   */
  public byte[] getCertOrCrl(String pathName) throws IOException, IllegalArgumentException {
    if (isCacheUpdateNeeded()) {
      loadCacheMap();  // load cache if not yet initialized
    }
    initializeCache();

    URL url = new URL(pathName);
    if (url == null) {
      throw new IllegalArgumentException("OnDieCache: illegal crl reference: " + pathName);
    }
    Path path = Paths.get(url.getFile());
    Path fileName = path.getFileName();
    if (fileName == null) {
      throw new IllegalArgumentException("OnDieCache: illegal crl reference: " + pathName);
    }
    return cacheMap.get(fileName.toString());
  }

}

