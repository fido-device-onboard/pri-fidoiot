// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.dispatch.KeyStoreInputStreamFunction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class FileKeyStoreInputStream implements KeyStoreInputStreamFunction {

  @Override
  public InputStream apply(String path) throws IOException {
    if (Path.of(path).toFile().exists()) {
      File inputFile = new File(path);
      if (inputFile.exists()) {
        return new FileInputStream(inputFile);
      }
    }
    return null;
  }
}
