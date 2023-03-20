// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.lang3.function.Failable;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.DeviceCredentialConsumer;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;

public class FileCredentialConsumer implements DeviceCredentialConsumer {
  private static final LoggerService logger = new LoggerService(FileCredentialConsumer.class);

  @Override
  public void accept(DeviceCredential deviceCredential) {
    try {
      DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();

      File credFile = new File(config.getCredentialFile());

      byte[] data = Mapper.INSTANCE.writeValue(deviceCredential);
      try (FileOutputStream out = new FileOutputStream(credFile)) {
        out.write(data);
      }
    } catch (FileNotFoundException e) {
      logger.error("Credential File Not Found ");
      throw Failable.rethrow(e);
    } catch (IOException e) {
      logger.error("Error reading credential file ");
      throw Failable.rethrow(e);
    }
  }


}
