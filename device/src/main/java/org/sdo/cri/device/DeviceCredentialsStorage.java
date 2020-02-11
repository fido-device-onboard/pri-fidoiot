// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri.device;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import org.sdo.cri.DeviceCredentials;
import org.sdo.cri.DeviceCredentialsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DeviceCredentialsStorage {

  private static final Logger myLogger = LoggerFactory.getLogger(DeviceCredentialsStorage.class);
  private final URI myInputUri;
  private final Path myOutputDir;

  DeviceCredentialsStorage(URI inputUri, Path outputDir) {
    myInputUri = inputUri;
    myOutputDir = outputDir;
  }

  DeviceCredentials load() {

    Path path = null != myInputUri ? Paths.get(myInputUri) : null;

    if (null != path) {
      try (Reader reader =
          new BufferedReader(new FileReader(path.toFile(), StandardCharsets.US_ASCII))) {
        return new DeviceCredentialsParser().readObject(reader);
      } catch (IOException e) {
        if (myLogger.isDebugEnabled()) {
          myLogger.debug(e.getMessage());
        }
      }
    }

    return null;
  }

  void store(DeviceCredentials deviceCredentials) throws IOException {

    if (null == myOutputDir) {
      myLogger.info("outputDir is null, credentials not saved");
      return;
    }

    if (!myOutputDir.toFile().isDirectory()) {
      Files.createDirectories(myOutputDir);
    }

    Path path = myOutputDir.resolve(deviceCredentials.getUuid() + ".oc");
    Set<OpenOption> openOptions = new HashSet<>();

    openOptions.add(StandardOpenOption.CREATE);
    openOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
    openOptions.add(StandardOpenOption.WRITE);

    try (
        FileChannel channel = FileChannel.open(path, openOptions);
        Writer writer = Channels.newWriter(channel, StandardCharsets.US_ASCII)) {
      writer.write(deviceCredentials.toString());
      myLogger.info("credentials saved to " + path.normalize().toAbsolutePath());
    }
  }
}
