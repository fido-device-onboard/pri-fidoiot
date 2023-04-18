// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.lang3.function.Failable;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialSupplier;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;


public class FileCredentialSupplier implements DeviceCredentialSupplier {
  private static final LoggerService logger = new LoggerService(FileCredentialSupplier.class);

  @Override
  public DeviceCredential get() throws IOException {
    try {
      DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();
      Path path = Path.of(config.getCredentialFile());
      if (path.toFile().exists()) {
        byte[] data = Files.readAllBytes(Path.of(config.getCredentialFile()));
        return Mapper.INSTANCE.readValue(data, DeviceCredential.class);
      }
      return null;

    } catch (FileNotFoundException e) {
      logger.error("Credential file not found ");
      throw Failable.rethrow(e);
    } catch (IOException e) {
      logger.error("Error in reading file ");
      throw Failable.rethrow(e);
    }
  }
}

