package org.fidoalliance.fdo.sample;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.FdoSimCommandOwnerModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;

public class FdoSimCommandDeviceModule implements ServiceInfoModule {

  private static final LoggerService logger = new LoggerService(FdoSimCommandDeviceModule.class);

  private final ProcessBuilder.Redirect execOutputRedirect = ProcessBuilder.Redirect.PIPE;
  private final ProcessBuilder.Redirect execNoOutput = Redirect.DISCARD;
  private final Duration execTimeout = Duration.ofHours(2);

  private final ServiceInfoQueue queue = new ServiceInfoQueue();

  private String command;
  private String[] commandArgs;
  private Process execProcess;
  private boolean mayFail;
  private boolean returnStdOut;
  private boolean returnStdErr;

  @Override
  public String getName() {
    return FdoSimCommandOwnerModule.MODULE_NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {

    command = null;
    commandArgs = null;
    mayFail = false;
    returnStdOut = false;
    returnStdErr = false;
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {

    switch (kvPair.getKey()) {
      case FdoSimCommandOwnerModule.ACTIVE:
        logger.info(FdoSimCommandOwnerModule.ACTIVE + " = "
            + Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        state.setActive(Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        break;
      case FdoSimCommandOwnerModule.RETURN_STDOUT:
        if (state.isActive()) {
          returnStdOut = Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class);
        }
        break;
      case FdoSimCommandOwnerModule.RETURN_STDERR:
        if (state.isActive()) {
          returnStdErr = Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class);
        }
        break;
      case FdoSimCommandOwnerModule.MAY_FAIL:
        if (state.isActive()) {
          mayFail = Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class);
        }
        break;
      case FdoSimCommandOwnerModule.SIG:
        if (state.isActive()) {
          int sigValue = Mapper.INSTANCE.readValue(kvPair.getValue(), Integer.class);
          logger.info("Process Signal received : " + sigValue);
          if ((sigValue == 9 || sigValue == 15) && execProcess != null) {
            execProcess.destroyForcibly();
          }
        }
        break;
      case FdoSimCommandOwnerModule.COMMAND:
        if (state.isActive()) {
          command = Mapper.INSTANCE.readValue(kvPair.getValue(), String.class);
          logger.info("command " + command);
        }
        break;
      case FdoSimCommandOwnerModule.ARGS:
        if (state.isActive()) {
          commandArgs = Mapper.INSTANCE.readValue(kvPair.getValue(), String[].class);
          logger.info("with args: " + Arrays.asList(commandArgs));
        }
        break;
      case FdoSimCommandOwnerModule.EXECUTE:
        if (state.isActive()) {
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

  private void checkStatus() throws IOException {

    if (execProcess != null) {
      if (execProcess.isAlive()) {
        if (returnStdErr) {

          execProcess.getErrorStream();

        }
        if (returnStdOut) {
          execProcess.getOutputStream();
        }
      } else {
        ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
        kv.setKeyName(FdoSimCommandOwnerModule.EXIT_CODE);
        kv.setValue(Mapper.INSTANCE.writeValue(execProcess.exitValue()));
        queue.add(kv);
        execProcess.destroy();
        execProcess = null;

      }
    }
  }

  private void execute() {
    List<String> argList = new ArrayList<>();
    argList.add(command);
    for (int i = 0; i < commandArgs.length; i++) {
      argList.add(commandArgs[i]);
    }

    try {
      ProcessBuilder builder = new ProcessBuilder(argList);
      builder.directory(new File(getAppData()));
      builder.redirectErrorStream(returnStdErr);

      builder.redirectOutput(getExecOutputRedirect());
      execProcess = builder.start();
      try {
        boolean processDone = execProcess.waitFor(
            getExecTimeout().toMillis(), TimeUnit.MILLISECONDS);
        if (processDone) {
          if (execProcess.exitValue() != 0) {
            throw new RuntimeException(
                "predicate failed: "
                    + getCommand(argList)
                    + " returned "
                    + execProcess.exitValue());
          }
        } else { // timeout
          logger.error("Process Timeout.");
        }

      } catch (InterruptedException e) {
        throw new InternalServerErrorException(e);
      } finally {
        if (execProcess.isAlive()) {
          execProcess.destroyForcibly();
        }
      }
    } catch (IOException e) {
      logger.error("IO Operation Failed" + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private String getAppData() {
    DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();
    File file = new File(config.getCredentialFile());
    return file.getParent();
  }

  private ProcessBuilder.Redirect getExecOutputRedirect() {
    if (returnStdOut) {
      return execOutputRedirect;
    }
    return execNoOutput;
  }

  private Duration getExecTimeout() {
    return execTimeout;
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
}
