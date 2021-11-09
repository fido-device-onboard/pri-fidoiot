// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
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

  private static final int DEFAULT_STATUS_TIMEOUT = 5; //seconds


  private ProcessBuilder.Redirect execOutputRedirect = ProcessBuilder.Redirect.PIPE;
  private Duration execTimeout = Duration.ofHours(2);
  private Predicate<Integer> exitValueTest = val -> (0 == val);
  private static final LoggerService logger = new LoggerService(DeviceSysModule.class);
  private Path currentFile;
  private boolean isActive;
  private int listIndex = 0;

  private Process execProcess;
  private int statusTimeout = DEFAULT_STATUS_TIMEOUT;
  private Composite statusMessage;

  private String fetchFileName;
  private long fetchOffset;
  private int mtu;


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
    this.mtu = mtu;
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
      case FdoSys.KEY_EXEC_CB:
        if (isActive) {
          exec_cb(kvPair.getAsComposite(Const.SECOND_KEY));
        } else {
          logger.warn("fdo_sys module not active. Ignoring fdo_sys:exec.");
        }
        break;
      case FdoSys.KEY_STATUS_CB:
        if (isActive) {
          Composite status = kvPair.getAsComposite(Const.SECOND_KEY);
          //update the timeout
          statusTimeout = status.getAsNumber(Const.THIRD_KEY).intValue();
          //kill the process if requested by owner
          if (status.getAsBoolean(Const.FIRST_KEY) && execProcess != null) {
            execProcess.destroyForcibly();
            execProcess = null;
            statusMessage = null;
          } else {
            checkStatus();
          }
        } else {
          logger.warn("fdo_sys module not active. Ignoring fdo_sys:exec.");
        }
        break;
      case FdoSys.KEY_FETCH:
        if (isActive) {
          fetchFileName = kvPair.getAsString(Const.SECOND_KEY);
          fetchOffset = 0;
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
    return (statusMessage != null || fetchFileName != null);
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public Composite nextMessage() {
    Composite result = null;
    if (statusMessage != null) {
      result = statusMessage;
      statusMessage = null;
    } else if (fetchFileName != null) {
      result = fetch();
    }
    return result;
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

  private Composite fetch() {
    try (FileInputStream in = new FileInputStream(fetchFileName)) {

      long skipped = 0;
      while (skipped < fetchOffset) {
        long r = in.skip(fetchOffset);
        skipped += r;
      }
      fetchOffset = skipped;
      byte[] data = new byte[mtu - 100];
      int br = in.read(data);
      fetchOffset = skipped + br;
      if (br < data.length && br >= 0) {
        //adjust buffer
        byte[] temp = new byte[br];
        System.arraycopy(data, 0, temp, 0, br);
        data = temp;
      }

      if (br >= 0) {
        return Composite.newArray()
            .set(Const.FIRST_KEY, FdoSys.KEY_DATA)
            .set(Const.SECOND_KEY, data);
      } else {
        fetchOffset = 0;
        fetchFileName = null;
        return Composite.newArray()
            .set(Const.FIRST_KEY, FdoSys.KEY_EOT)
            .set(Const.SECOND_KEY, Composite.newArray().set(0, 0));
      }

    } catch (FileNotFoundException e) {
      logger.debug("fetch file not found");
    } catch (IOException e) {
      logger.debug("error reading fetch file");
    }
    fetchOffset = 0;
    fetchFileName = null;
    return Composite.newArray()
        .set(Const.FIRST_KEY, FdoSys.KEY_EOT)
        .set(Const.SECOND_KEY, Composite.newArray().set(0, 1));
  }

  private void checkStatus() {
    //check if finished
    if (execProcess != null) {
      if (!execProcess.isAlive()) {
        statusMessage = createStatus(true, execProcess.exitValue(), statusTimeout);
        execProcess = null;
        return;
      }
    }

    //process still running
    if (execProcess != null) {
      try {
        execProcess.waitFor(statusTimeout, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        logger.warn("timer interrupted");
      }
      statusMessage = createStatus(false, 0, statusTimeout);
    }
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


  private void exec_cb(Composite args) {

    List<String> argList = new ArrayList<>();
    for (int i = 0; i < args.size(); i++) {
      argList.add(args.getAsString(i));
    }

    try {
      ProcessBuilder builder = new ProcessBuilder(argList);
      builder.redirectErrorStream(true);
      builder.redirectOutput(getExecOutputRedirect());
      execProcess = builder.start();
      statusTimeout = DEFAULT_STATUS_TIMEOUT;
      //set the first status check
      statusMessage = createStatus(false, 0, statusTimeout);
    } catch (IOException e) {
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

  private Composite createStatus(boolean completed, int retCode, int timeout) {
    Composite status = Composite.newArray()
        .set(Const.FIRST_KEY, completed)
        .set(Const.SECOND_KEY, retCode)
        .set(Const.THIRD_KEY, timeout);
    return Composite.newArray()
        .set(Const.FIRST_KEY, FdoSys.KEY_STATUS_CB)
        .set(Const.SECOND_KEY, status);
  }

}
