// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.fidoalliance.fdo.protocol.Composite;

class CredentialStorage {

  private final Path myPath;

  CredentialStorage(Path path) {
    myPath = path;
  }

  Composite load() throws IOException {
    if (null != myPath) {

      try (FileInputStream in = new FileInputStream(myPath.toFile())) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        in.transferTo(bytes);
        return Composite.fromObject(bytes.toByteArray());

      }
    } else {
      return null;
    }
  }

  void store(Composite c) throws IOException {
    if (!(null == myPath || null == c)) {
      try (FileOutputStream out = new FileOutputStream(myPath.toFile())) {
        out.write(c.toBytes());
      }
    }
  }
}
