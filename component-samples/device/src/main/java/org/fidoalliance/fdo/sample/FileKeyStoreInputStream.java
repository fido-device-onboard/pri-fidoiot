// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.fidoalliance.fdo.protocol.dispatch.KeyStoreInputStreamFunction;

public class FileKeyStoreInputStream implements KeyStoreInputStreamFunction {


  @Override
  public InputStream apply(String path) throws IOException {
    File inputFile = new File(path);
    if (inputFile.exists()) {
      return new FileInputStream(inputFile);
    }
    return null;
  }
}
