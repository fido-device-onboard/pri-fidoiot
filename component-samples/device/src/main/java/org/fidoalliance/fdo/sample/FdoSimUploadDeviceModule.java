// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.FdoSimUploadOwnerModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;

public class FdoSimUploadDeviceModule implements ServiceInfoModule {

  private static final LoggerService logger = new LoggerService(FdoSimUploadDeviceModule.class);


  private MessageDigest digest;
  private boolean needSha;

  private final ServiceInfoQueue queue = new ServiceInfoQueue();

  @Override
  public String getName() {
    return FdoSimUploadOwnerModule.MODULE_NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {
    digest = null;
    needSha = false;
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {
    switch (kvPair.getKey()) {
      case FdoSimUploadOwnerModule.ACTIVE:
        logger.info(FdoSimUploadOwnerModule.ACTIVE + " = "
            + Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        state.setActive(Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        break;
      case FdoSimUploadOwnerModule.NAME:
        if (state.isActive()) {
          String fetchFileName = Mapper.INSTANCE.readValue(kvPair.getValue(), String.class);
          fetch(fetchFileName, state.getMtu());
        } else {
          logger.warn(FdoSimUploadOwnerModule.MODULE_NAME + " not active");
        }
        break;
      case FdoSimUploadOwnerModule.NEED_SHA:
        if (state.isActive()) {
          needSha = Mapper.INSTANCE.readValue(kvPair.getValue(), boolean.class);

        }
        break;
      default:
        break;
    }
  }

  @Override
  public void keepAlive() throws IOException {

  }

  @Override
  public void send(ServiceInfoModuleState state, ServiceInfoSendFunction sendFunction)
      throws IOException {
    while (queue.size() > 0) {
      boolean sent = sendFunction.apply(queue.peek());
      if (sent) {
        queue.poll();
      } else {
        break;
      }
    }

  }

  private String getAppData() {
    DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();
    File file = new File(config.getCredentialFile());
    return file.getParent();
  }

  private void fetch(String fetchFileName, int mtu) throws IOException {

    String fileName = fetchFileName;
    if (!Path.of(fetchFileName).isAbsolute()) {
      fileName = Path.of(getAppData(), fetchFileName).toString();
    }

    try (FileInputStream in = new FileInputStream(fileName)) {

      byte[] data = new byte[mtu - 100];
      int fileLength = 0;

      for (; ; ) {
        int br = in.read(data);
        if (br < 0) {
          break;
        }
        if (br < data.length && br >= 0) {
          //adjust buffer
          byte[] temp = new byte[br];
          System.arraycopy(data, 0, temp, 0, br);
          data = temp;
        }

        if (data.length > 0) {

          fileLength += data.length;
          if (needSha) {
            if (digest == null) {
              digest = MessageDigest.getInstance("SHA-384");
            }
            digest.update(data);
          }

          ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(FdoSimUploadOwnerModule.DATA);
          kv.setValue(Mapper.INSTANCE.writeValue(data));
          queue.add(kv);
        }
      }

      ServiceInfoKeyValuePair emptyKeyPair = new ServiceInfoKeyValuePair();
      emptyKeyPair.setKeyName(FdoSimUploadOwnerModule.DATA);
      emptyKeyPair.setValue(Mapper.INSTANCE.writeValue(new byte[0]));
      queue.add(emptyKeyPair);

      if (needSha) {

        ServiceInfoKeyValuePair shaKeyPair = new ServiceInfoKeyValuePair();
        shaKeyPair.setKeyName(FdoSimUploadOwnerModule.SHA_384);
        if (digest == null) {
          digest = MessageDigest.getInstance("SHA-384");
        }
        shaKeyPair.setValue(Mapper.INSTANCE.writeValue(digest.digest()));
        queue.add(shaKeyPair);
        digest = null;
      }

      ServiceInfoKeyValuePair lengthKeyPair = new ServiceInfoKeyValuePair();
      lengthKeyPair.setKeyName(FdoSimUploadOwnerModule.LENGTH);
      lengthKeyPair.setValue(Mapper.INSTANCE.writeValue(fileLength));
      queue.addFirst(lengthKeyPair);


    } catch (FileNotFoundException e) {
      logger.debug(fetchFileName + " fetch file not found");
    } catch (IOException e) {
      logger.debug(fetchFileName + "error reading fetch file");
    } catch (NoSuchAlgorithmException e) {
      logger.debug(fetchFileName + e.getMessage());
    }
  }


}
