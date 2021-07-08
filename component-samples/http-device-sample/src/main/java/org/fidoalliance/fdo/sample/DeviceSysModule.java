// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.ServiceInfoEncoder;
import org.fidoalliance.fdo.serviceinfo.DevMod;
import org.fidoalliance.fdo.serviceinfo.FdoSys;
import org.fidoalliance.fdo.serviceinfo.Module;

/**
 * Implements the device side of the FdoSys Module.
 */
public class DeviceSysModule implements Module {

  // Keys for localized log format strings in our resource bundle
  private static final String LOG_ERR_PATH_NULL =
      DeviceSysModule.class.getName() + ".LOG_ERR_PATH_NULL";
  private static final String LOG_INFO_FILE_CREATED =
      DeviceSysModule.class.getName() + ".LOG_INFO_FILE_CREATED";


  private ProcessBuilder.Redirect execOutputRedirect = ProcessBuilder.Redirect.PIPE;
  private Duration execTimeout = Duration.ofHours(2);
  private Predicate<Integer> exitValueTest = val -> (0 == val);
  private static final LoggerService logger = new LoggerService(DeviceSysModule.class);
  private Path currentFile;
  private boolean isActive;
  private int listIndex = 0;

  @Override
  public String getName() {
    return FdoSys.NAME;
  }

  @Override
  public void prepare(UUID guid) {
    currentFile = null;
    isActive = false;
  }

  @Override
  public void setMtu(int mtu) {

  }

  @Override
  public void setState(Composite state) {

  }

  @Override
  public Composite getState() {
    return null;
  }

  @Override
  public void setServiceInfo(Composite kvPair, boolean isMore) {
    String name = kvPair.getAsString(Const.FIRST_KEY);
    switch (name) {
      case FdoSys.KEY_ACTIVE:
        isActive = kvPair.getAsBoolean(Const.SECOND_KEY);
        break;
      case FdoSys.KEY_FILEDESC:
        if (isActive) {
          createFile(Path.of(kvPair.getAsString(Const.SECOND_KEY)));
        } else {
          logger.warn("fdo_sys module not active. Ignoring fdo_sys:filedesc.");
        }
        break;
      case FdoSys.KEY_WRITE:
        if (isActive) {
          writeFile(kvPair.getAsBytes(Const.SECOND_KEY));
        } else {
          logger.warn("fdo_sys module not active. Ignoring fdo_sys:filewrite.");
        }
        break;
      case FdoSys.KEY_EXEC:
        if (isActive) {
          exec(kvPair.getAsComposite(Const.SECOND_KEY));
        } else {
          logger.warn("fdo_sys module not active. Ignoring fdo_sys:exec.");
        }
        break;
      default:
        break;
    }
  }

  public boolean isMore() {
    return false;
  }

  @Override
  public boolean hasMore() {
    return false;
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public Composite nextMessage() {
    return Composite.newArray();
  }

  private void setPath(Path path) {
    this.currentFile = path;
  }

  private Path getPath() {
    return this.currentFile;
  }

  private void createFile(Path path) {

    Set<OpenOption> openOptions = new HashSet<>();
    openOptions.add(StandardOpenOption.CREATE);
    openOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
    openOptions.add(StandardOpenOption.WRITE);

    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {

      Set<PosixFilePermission> filePermissions = new HashSet<>();
      filePermissions.add(PosixFilePermission.OWNER_READ);
      filePermissions.add(PosixFilePermission.OWNER_WRITE);
      FileAttribute<?> fileAttribute = PosixFilePermissions.asFileAttribute(filePermissions);

      try (FileChannel channel = FileChannel.open(path, openOptions, fileAttribute)) {
        logger.info(LOG_INFO_FILE_CREATED);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    } else {

      try (FileChannel channel = FileChannel.open(path, openOptions)) {
        // opening the channel is enough to create the file
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    setPath(path);
  }


  private void writeFile(byte[] data) {

    Path path = getPath();
    if (null == path) {
      throw new IllegalStateException(LOG_ERR_PATH_NULL);
    }

    try {
      try (FileChannel channel = FileChannel
          .open(getPath(), StandardOpenOption.APPEND)) {

        channel.write(ByteBuffer.wrap(data));

      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getCommand(List<String> args) {
    StringBuilder builder = new StringBuilder();
    for (String arg : args) {
      if (builder.length() > 0) {
        builder.append(" ");
      }
      builder.append(arg);
    }
    return builder.toString();
  }


  private void exec(Composite args) {

    List<String> argList = new ArrayList<>();
    for (int i = 0; i < args.size(); i++) {
      argList.add(args.getAsString(i));
    }

    try {
      ProcessBuilder builder = new ProcessBuilder(argList);
      builder.redirectErrorStream(true);
      builder.redirectOutput(getExecOutputRedirect());
      Process process = builder.start();
      try {
        boolean processDone = process.waitFor(getExecTimeout().toMillis(), TimeUnit.MILLISECONDS);
        if (processDone) {
          if (!getExitValueTest().test(process.exitValue())) {
            throw new RuntimeException(
                "predicate failed: "
                    + getCommand(argList)
                    + " returned "
                    + process.exitValue());
          }
        } else { // timeout
          throw new TimeoutException(getCommand(argList));
        }

      } finally {

        if (process.isAlive()) {
          process.destroyForcibly();
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private ProcessBuilder.Redirect getExecOutputRedirect() {
    return execOutputRedirect;
  }

  private Duration getExecTimeout() {
    return execTimeout;
  }

  private Predicate<Integer> getExitValueTest() {
    return exitValueTest;
  }

}
