// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.FdoSimDownloadOwnerModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;

public class FdoSimDownloadDeviceModule implements ServiceInfoModule {

  private static final LoggerService logger = new LoggerService(FdoSimDownloadDeviceModule.class);

  private Path currentFile;
  private byte[] expectedCheckSum;
  private Integer expectedLength;
  private int bytesReceived;
  private MessageDigest digest;

  private final ServiceInfoQueue queue = new ServiceInfoQueue();

  @Override
  public String getName() {
    return FdoSimDownloadOwnerModule.MODULE_NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {
    currentFile = null;
    expectedCheckSum = null;
    expectedLength = null;
    bytesReceived = 0;
    digest = null;
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {
    switch (kvPair.getKey()) {
      case FdoSimDownloadOwnerModule.ACTIVE:
        logger.info(FdoSimDownloadOwnerModule.ACTIVE + " = "
            + Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        state.setActive(Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        break;
      case FdoSimDownloadOwnerModule.NAME:
        if (state.isActive()) {
          String fileDesc = Mapper.INSTANCE.readValue(kvPair.getValue(), String.class);
          logger.info("File created on path: " + fileDesc);
          createFile(Path.of(fileDesc));
        }
        break;
      case FdoSimDownloadOwnerModule.DATA:
        if (state.isActive()) {
          byte[] data = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          writeFile(data);

        }
        break;
      case FdoSimDownloadOwnerModule.LENGTH:
        if (state.isActive()) {
          expectedLength = Mapper.INSTANCE.readValue(kvPair.getValue(), Integer.class);
          bytesReceived = 0;
          logger.info("expected file length " + expectedLength);
        }
        break;
      case FdoSimDownloadOwnerModule.SHA_384:
        if (state.isActive()) {
          expectedCheckSum = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          try {
            digest = MessageDigest.getInstance("SHA-384");
            logger.info("expected checksum "
                + Hex.encodeHexString(expectedCheckSum, false));
          } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
          }

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

  private void createFile(Path path) {

    currentFile = path;
    if (!path.isAbsolute()) {
      currentFile = Path.of(getAppData(), path.toString());
    }
    Set<OpenOption> openOptions = new HashSet<>();
    openOptions.add(StandardOpenOption.CREATE);
    openOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
    openOptions.add(StandardOpenOption.WRITE);

    boolean posixType = false;
    try (FileSystem fs = FileSystems.getDefault()) {
      posixType = fs.supportedFileAttributeViews().contains("posix");
    } catch (RuntimeException e) {
      posixType = false;
    } catch (Exception e) {
      posixType = false;
    }

    if (posixType) {

      Set<PosixFilePermission> filePermissions = new HashSet<>();
      filePermissions.add(PosixFilePermission.OWNER_READ);
      filePermissions.add(PosixFilePermission.OWNER_WRITE);
      FileAttribute<?> fileAttribute = PosixFilePermissions.asFileAttribute(filePermissions);

      try (FileChannel channel = FileChannel.open(currentFile, openOptions, fileAttribute)) {
        logger.info(FdoSimDownloadOwnerModule.NAME + " file created.");
      } catch (IOException e) {
        currentFile = null;
        throw new RuntimeException(e);
      }

    } else {

      try (FileChannel channel = FileChannel.open(currentFile, openOptions)) {
        // opening the channel is enough to create the file
      } catch (IOException e) {
        currentFile = null;
        throw new RuntimeException(e);
      }

    }


  }


  private void writeFile(byte[] data) throws IOException {

    if (null == currentFile) {
      throw new InternalServerErrorException(FdoSimDownloadOwnerModule.NAME + " not provided");
    }

    if (null == expectedLength) {
      throw new InternalServerErrorException(FdoSimDownloadOwnerModule.LENGTH + " not provided");
    }

    if (bytesReceived == expectedLength) {
      return;
    }

    try (FileChannel channel = FileChannel.open(currentFile, StandardOpenOption.APPEND)) {

      bytesReceived += channel.write(ByteBuffer.wrap(data));

      if (digest != null) {
        digest.update(data);
      }

      int returnCode = -1;
      if (bytesReceived == expectedLength) {

        if (digest != null) {
          byte[] checkSum = digest.digest();
          if (ByteBuffer.wrap(checkSum).compareTo(ByteBuffer.wrap(expectedCheckSum)) == 0) {
            returnCode = bytesReceived;
          }
        } else {
          returnCode = bytesReceived;
        }

        ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
        kv.setKeyName(FdoSimDownloadOwnerModule.DONE);
        kv.setValue(Mapper.INSTANCE.writeValue(returnCode));
        queue.add(kv);
      }

    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }

  }
}
