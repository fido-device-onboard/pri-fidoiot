// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.FdoSimWebGetOwnerModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;

public class FdoSimWebGetDeviceModule implements ServiceInfoModule {

  private static final LoggerService logger = new LoggerService(FdoSimWebGetDeviceModule.class);
  private final ServiceInfoQueue queue = new ServiceInfoQueue();

  private String name;
  private String url;
  private byte[] sha384;
  private byte[] buffer;
  private int totalBytes;


  private CloseableHttpClient httpClient;
  private CloseableHttpResponse httpResponse;
  private InputStream contentStream;
  private FileChannel fileChannel;
  private MessageDigest digest;

  @Override
  public String getName() {
    return FdoSimWebGetOwnerModule.MODULE_NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {

    name = null;
    url = null;
    sha384 = null;
    buffer = null;
    totalBytes = 0;

    closeConnection();
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {

    switch (kvPair.getKey()) {
      case FdoSimWebGetOwnerModule.ACTIVE:
        logger.info(FdoSimWebGetOwnerModule.ACTIVE + " = "
            + Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        state.setActive(Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        break;
      case FdoSimWebGetOwnerModule.SHA_384:
        if (state.isActive()) {
          sha384 = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          try {
            digest = MessageDigest.getInstance("SHA-384");
            logger.info("expected checksum "
                + Hex.encodeHexString(sha384, false));
          } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
          }
        }
        break;
      case FdoSimWebGetOwnerModule.NAME:
        if (state.isActive()) {
          name = Mapper.INSTANCE.readValue(kvPair.getValue(), String.class);
        }
        break;
      case FdoSimWebGetOwnerModule.URL:
        if (state.isActive()) {
          url = Mapper.INSTANCE.readValue(kvPair.getValue(), String.class);
          execute();
        }
        break;

      default:
        checkStatus();
        break;
    }
  }

  @Override
  public void keepAlive() throws IOException {

    checkStatus();
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


  private void closeConnection() throws IOException {
    if (httpResponse != null) {
      httpResponse.close();
      httpResponse = null;
    }

    if (httpClient != null) {
      httpClient.close();
      httpClient = null;
    }

    if (contentStream != null) {
      contentStream.close();
      contentStream = null;
    }

    if (fileChannel != null) {
      fileChannel.close();
      fileChannel = null;
    }
    buffer = null;
    digest = null;
    sha384 = null;
  }

  private void checkStatus() throws IOException {

    if (buffer != null) {
      int br = contentStream.read(buffer);

      if (br < 0) {
        ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
        kv.setKeyName(FdoSimWebGetOwnerModule.DONE);
        kv.setValue(Mapper.INSTANCE.writeValue(totalBytes));
        queue.add(kv);

        if (digest != null) {
          kv = new ServiceInfoKeyValuePair();
          byte[] computed = digest.digest();
          if (!java.util.Arrays.equals(computed, sha384)) {
            kv.setKeyName(FdoSimWebGetOwnerModule.ERROR);
            kv.setValue(Mapper.INSTANCE.writeValue("SHA mismatch"));
            queue.add(kv);

          } else {
            kv.setKeyName(FdoSimWebGetOwnerModule.SHA_384);
            kv.setValue(Mapper.INSTANCE.writeValue(computed));
            queue.add(kv);
          }
        }
        closeConnection();

      } else {

        totalBytes += br;
        byte[] data = buffer;
        if (br < buffer.length) {
          data = new byte[br];
          System.arraycopy(buffer, 0, data, 0, br);
        }
        fileChannel.write(ByteBuffer.wrap(data));
        if (digest != null) {
          digest.update(data);
        }
      }
    }
  }

  private void execute() throws IOException {

    httpClient = HttpClients.createDefault();

    HttpGet httpRequest = new HttpGet(url);
    CloseableHttpResponse httpResponse = httpClient.execute(httpRequest);

    StatusLine statusLine = httpResponse.getStatusLine();

    if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
      ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
      kv.setKeyName(FdoSimWebGetOwnerModule.ERROR);
      kv.setValue(Mapper.INSTANCE.writeValue(statusLine.getReasonPhrase()));
      queue.add(kv);
      closeConnection();
    } else {
      contentStream = httpResponse.getEntity().getContent();
      createFile();
      buffer = new byte[2048];
    }


  }

  private String getAppData() {
    DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();
    File file = new File(config.getCredentialFile());
    return file.getParent();
  }

  private void createFile() throws IOException {

    Path currentFile = null;
    if (name == null) {
      int pos = url.lastIndexOf("/");
      if (pos > 0) {
        currentFile = Path.of(url.substring(pos + 1));
      } else {
        currentFile = Path.of("default");
      }

    } else {
      currentFile = Path.of(name);
    }

    if (!currentFile.isAbsolute()) {
      currentFile = Path.of(getAppData(), currentFile.toString());
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

      fileChannel = FileChannel.open(currentFile, openOptions, fileAttribute);


    } else {

      fileChannel = FileChannel.open(currentFile, openOptions);

    }


  }


}
