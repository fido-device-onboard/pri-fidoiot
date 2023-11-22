// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.DevModList;
import org.fidoalliance.fdo.protocol.message.ServiceInfoDocument;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;
import org.fidoalliance.fdo.protocol.serviceinfo.FdoSys;

public class FdoSimCommandOwnerModule implements ServiceInfoModule {


  public static final String MODULE_NAME = "fdo.command";
  public static final String ACTIVE = MODULE_NAME + ":active";
  public static final String COMMAND = MODULE_NAME + ":command";
  public static final String ARGS = MODULE_NAME + ":args";
  public static final String MAY_FAIL = MODULE_NAME + ":may_fail";
  public static final String RETURN_STDOUT = MODULE_NAME + ":return_stdout";
  public static final String RETURN_STDERR = MODULE_NAME + ":return_stderr";
  public static final String EXECUTE = MODULE_NAME + ":execute";
  public static final String SIG = MODULE_NAME + ":sig";
  public static final String STDOUT = MODULE_NAME + ":stdout";
  public static final String STDERR = MODULE_NAME + ":stderr";
  public static final String EXIT_CODE = MODULE_NAME + ":exitcode";
  public static final String BOOLEAN_TRUE = "true";

  private final LoggerService logger = new LoggerService(FdoSimCommandOwnerModule.class);

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {
    state.setExtra(AnyType.fromObject(new FdoSysModuleExtra()));
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {
    FdoSysModuleExtra extra = state.getExtra().covertValue(FdoSysModuleExtra.class);
    switch (kvPair.getKey()) {
      case DevMod.KEY_MODULES: {
        DevModList list =
            Mapper.INSTANCE.readValue(kvPair.getValue(), DevModList.class);
        for (String name : list.getModulesNames()) {
          if (name.equals(MODULE_NAME)) {
            state.setActive(true);


          }
        }
      }
      break;
      case DevMod.KEY_DEVICE:
      case DevMod.KEY_OS:
      case DevMod.KEY_VERSION:
      case DevMod.KEY_ARCH:
        extra.getFilter().put(kvPair.getKey(),
            Mapper.INSTANCE.readValue(kvPair.getValue(), String.class));
        break;

      case EXIT_CODE:
        if (state.isActive()) {
          int exitCode = Mapper.INSTANCE.readValue(kvPair.getValue(), Integer.class);

          logger.info(extra.getName() + " exit code " + exitCode);
          extra.setWaiting(false);
        }
        break;
      case STDOUT:
        if (state.isActive()) {
          byte[] data = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          logger.info(extra.getName() + " stdout received");

        }
        break;
      case RETURN_STDERR:
        if (state.isActive()) {
          byte[] data = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          logger.info(extra.getName() + " stderr received");
        }
        break;
      default:
        break;
    }
    state.setExtra(AnyType.fromObject(extra));
  }

  @Override
  public void keepAlive() throws IOException {

  }


  @Override
  public void send(ServiceInfoModuleState state, ServiceInfoSendFunction sendFunction)
      throws IOException {

    FdoSysModuleExtra extra = state.getExtra().covertValue(FdoSysModuleExtra.class);

    if (!extra.isLoaded() && infoReady(extra)) {
      load(state, extra);
      extra.setLoaded(true);
    }

    while (!state.getGlobalState().getQueue().isEmpty()) {
      boolean sent = sendFunction.apply(state.getGlobalState().getQueue().peek());
      if (sent) {
        checkWaiting(extra, Objects.requireNonNull(state.getGlobalState().getQueue().poll()));
      } else {
        break;
      }
      if (extra.isWaiting()) {
        break;
      }
    }
    if (state.getGlobalState().getQueue().size() == 0 && !extra.isWaiting()) {
      state.setDone(true);
    }
    state.setExtra(AnyType.fromObject(extra));
  }


  protected void checkWaiting(FdoSysModuleExtra extra, ServiceInfoKeyValuePair kv)
      throws IOException {
    switch (kv.getKey()) {
      case EXECUTE:
        extra.setWaiting(true);
        break;
      default:
        break;
    }
  }

  protected boolean infoReady(FdoSysModuleExtra extra) {
    return extra.getFilter().containsKey(DevMod.KEY_DEVICE)
        && extra.getFilter().containsKey(DevMod.KEY_OS)
        && extra.getFilter().containsKey(DevMod.KEY_VERSION)
        && extra.getFilter().containsKey(DevMod.KEY_ARCH);
  }

  protected boolean checkFilter(Map<String, String> devMap, Map<String, String> filterMap) {
    return !devMap.entrySet().containsAll(filterMap.entrySet());
  }

  protected boolean checkProvider(FdoSysInstruction instruction) {
    if (instruction.getModule() == null) {
      return false;
    }
    return instruction.getModule().equals(getName());
  }


  protected void load(ServiceInfoModuleState state, FdoSysModuleExtra extra)
      throws IOException {

    if (!state.isActive()) {
      return;
    }

    ServiceInfoDocument document = state.getDocument();
    if (!state.getActiveSent()) {
      ServiceInfoKeyValuePair activePair = new ServiceInfoKeyValuePair();
      activePair.setKeyName(ACTIVE);
      activePair.setValue(Mapper.INSTANCE.writeValue(state.isActive()));
      state.setActiveSent(true);
      state.getGlobalState().getQueue().addFirst(activePair);
    }
    if (document.getInstructions() != null) {
      FdoSysInstruction[] instructions =
              Mapper.INSTANCE.readJsonValue(document.getInstructions(), FdoSysInstruction[].class);

      boolean skip = false;
      for (int i = 0; i < instructions.length; i++) {

        if (!checkProvider(instructions[i])) {
          continue;
        }

        if (instructions[i].getFilter() != null) {
          skip = checkFilter(extra.getFilter(), instructions[i].getFilter());
        }
        if (skip) {
          document.setIndex(i);
          continue;
        }

        document.setIndex(i);

        if (instructions[i].getExecCbArgs() != null) {
          extra.setName(instructions[i].getExecCbArgs()[0]);

          ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(COMMAND);
          kv.setValue(Mapper.INSTANCE.writeValue(instructions[i].getExecCbArgs()[0]));
          state.getGlobalState().getQueue().add(kv);

          //build remaining args
          String[] commandArgs = new String[instructions[i].getExecCbArgs().length - 1];
          int index = 0;
          for (int j = 1; j < instructions[i].getExecCbArgs().length; j++) {
            commandArgs[index++] = instructions[i].getExecCbArgs()[j];
          }

          kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(ARGS);
          kv.setValue(Mapper.INSTANCE.writeValue(commandArgs));
          state.getGlobalState().getQueue().add(kv);

          if (instructions[i].getMayFail() != null) {
            kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(MAY_FAIL);
            if (instructions[i].getMayFail().compareToIgnoreCase(BOOLEAN_TRUE) == 0) {
              kv.setValue(Mapper.INSTANCE.writeValue(true));
            } else {
              kv.setValue(Mapper.INSTANCE.writeValue(false));
            }
            state.getGlobalState().getQueue().add(kv);
          } else {
            kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(MAY_FAIL);
            kv.setValue(Mapper.INSTANCE.writeValue(false));
            state.getGlobalState().getQueue().add(kv);
          }

          if (instructions[i].getReturnStdOut() != null) {
            kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(RETURN_STDOUT);
            if (instructions[i].getReturnStdErr().compareToIgnoreCase(BOOLEAN_TRUE) == 0) {
              kv.setValue(Mapper.INSTANCE.writeValue(true));
            } else {
              kv.setValue(Mapper.INSTANCE.writeValue(false));
            }
            state.getGlobalState().getQueue().add(kv);
          } else {
            kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(RETURN_STDOUT);
            kv.setValue(Mapper.INSTANCE.writeValue(false));
            state.getGlobalState().getQueue().add(kv);
          }

          if (instructions[i].getReturnStdErr() != null) {
            kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(RETURN_STDERR);
            if (instructions[i].getReturnStdErr().compareToIgnoreCase(BOOLEAN_TRUE) == 0) {
              kv.setValue(Mapper.INSTANCE.writeValue(true));
            } else {
              kv.setValue(Mapper.INSTANCE.writeValue(false));
            }
            state.getGlobalState().getQueue().add(kv);
          } else {
            kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(RETURN_STDERR);
            kv.setValue(Mapper.INSTANCE.writeValue(false));
            state.getGlobalState().getQueue().add(kv);
          }

          kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(EXECUTE);
          kv.setValue(Mapper.INSTANCE.writeValue(commandArgs));
          state.getGlobalState().getQueue().add(kv);



        }

      }

    }
  }


}
