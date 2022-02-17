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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.db.FdoSysModuleExtra;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.message.StatusCb;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;
import org.fidoalliance.fdo.protocol.serviceinfo.FdoSys;


public class FdoSysDeviceModule implements ServiceInfoModule {

  private final LoggerService logger = new LoggerService(DeviceApp.class);
  ;

  private ProcessBuilder.Redirect execOutputRedirect = ProcessBuilder.Redirect.PIPE;
  private Duration execTimeout = Duration.ofHours(2);
  private Predicate<Integer> exitValueTest = val -> (0 == val);
  private static final int DEFAULT_STATUS_TIMEOUT = 5; //seconds

  private Path currentFile;
  private Process execProcess;
  private int statusTimeout = DEFAULT_STATUS_TIMEOUT;
  ServiceInfoQueue queue = new ServiceInfoQueue();


  @Override
  public String getName() {
    return FdoSys.NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {
    currentFile = null;
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {
    switch (kvPair.getKey()) {
      case FdoSys.ACTIVE:
        logger.info(FdoSys.ACTIVE + " = "
            + Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        state.setActive(Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        break;
      case FdoSys.FILEDESC:
        if (state.isActive()) {
          String fileDesc = Mapper.INSTANCE.readValue(kvPair.getValue(), String.class);
          createFile(Path.of(fileDesc));
        }
        break;
      case FdoSys.WRITE:
        if (state.isActive()) {
          byte[] data = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          writeFile(data);
        }
        break;
      case FdoSys.EXEC:
        if (state.isActive()) {
          String[] args = Mapper.INSTANCE.readValue(kvPair.getValue(), String[].class);
          exec(args);
        }
        break;
      case FdoSys.EXEC_CB:
        if (state.isActive()) {
          String[] args = Mapper.INSTANCE.readValue(kvPair.getValue(), String[].class);
          exec_cb(args);
        }
        break;
      case FdoSys.STATUS_CB:
        if (state.isActive()) {
          StatusCb status = Mapper.INSTANCE.readValue(kvPair.getValue(),StatusCb.class);
          statusTimeout = status.getTimeout();
          if (status.isCompleted() && execProcess != null) {
            execProcess.destroyForcibly();
            execProcess = null;
          } else {
            checkStatus();
          }
        }
        break;
      default:
        break;
    }
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
        logger.info(FdoSys.FILEDESC + " file created.");
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

    currentFile = path;
  }


  private void writeFile(byte[] data) throws IOException {

    if (null == currentFile) {
      throw new InternalServerErrorException(FdoSys.FILEDESC + " not provided");
    }

    try {
      try (FileChannel channel = FileChannel
          .open(currentFile, StandardOpenOption.APPEND)) {

        channel.write(ByteBuffer.wrap(data));

      }
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }


  private void exec(String[] args) throws IOException {

    List<String> argList = Arrays.asList(args);

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
      throw new InternalServerErrorException(e);
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    } catch (TimeoutException e) {
      throw new InternalServerErrorException(e);
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

  private void exec_cb(String[] args) throws IOException {

    List<String> argList = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      argList.add(args[i]);
    }

    try {
      ProcessBuilder builder = new ProcessBuilder(argList);
      builder.redirectErrorStream(true);
      builder.redirectOutput(getExecOutputRedirect());
      execProcess = builder.start();
      statusTimeout = DEFAULT_STATUS_TIMEOUT;
      //set the first status check
      createStatus(false, 0, statusTimeout);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private void checkStatus() throws IOException {
    //check if finished
    if (execProcess != null) {
      if (!execProcess.isAlive()) {
        createStatus(true, execProcess.exitValue(), statusTimeout);
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
      createStatus(false, 0, statusTimeout);
    }
  }

  private void createStatus(boolean completed, int retCode, int timeout) throws IOException {

    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(FdoSys.STATUS_CB);

    StatusCb status = new StatusCb();
    status.setCompleted(completed);
    status.setRetCode(retCode);
    status.setTimeout(timeout);
    kv.setValue(Mapper.INSTANCE.writeValue(status));
    queue.add(kv);

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
