// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
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

public class FdoSimWebGetOwnerModule implements ServiceInfoModule {


  public static final String MODULE_NAME = "fdo.wget";
  public static final String ACTIVE = MODULE_NAME + ":active";
  public static final String SHA_384 = MODULE_NAME + ":sha-384";
  public static final String NAME = MODULE_NAME + ":name";
  public static final String URL = MODULE_NAME + ":url";
  public static final String ERROR = MODULE_NAME + ":error";
  public static final String DONE = MODULE_NAME + ":done";


  private final LoggerService logger = new LoggerService(FdoSimWebGetOwnerModule.class);

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

      case ERROR:
        if (state.isActive()) {
          String errorString = Mapper.INSTANCE.readValue(kvPair.getValue(), String.class);

          logger.info(extra.getName() + " error " + errorString);
          extra.setWaiting(false);
          throw new InternalServerErrorException(errorString);
        }
        break;
      case DONE:
        if (state.isActive()) {
          int received = Mapper.INSTANCE.readValue(kvPair.getValue(), Integer.class);
          logger.info(received + " bytes received");
          extra.setWaiting(false);


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
    if (state.getGlobalState().getQueue().isEmpty() && !extra.isWaiting()) {
      state.setDone(true);
    }
    state.setExtra(AnyType.fromObject(extra));
  }


  protected void checkWaiting(FdoSysModuleExtra extra, ServiceInfoKeyValuePair kv)
      throws IOException {
    switch (kv.getKey()) {
      case URL:
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
    if (instruction.getModule() != null) {
      return instruction.getModule().equals(getName());
    }
    return true;
  }


  protected void load(ServiceInfoModuleState state, FdoSysModuleExtra extra)
      throws IOException {

    if (!state.isActive()) {
      return;
    }

    ServiceInfoDocument document = state.getDocument();
    FdoSysInstruction[] instructions =
        Mapper.INSTANCE.readJsonValue(document.getInstructions(), FdoSysInstruction[].class);

    boolean skip = false;
    for (int i = document.getIndex(); i < instructions.length; i++) {

      if (!checkProvider(instructions[i])) {
        break;
      }

      if (instructions[i].getFilter() != null) {
        skip = checkFilter(extra.getFilter(), instructions[i].getFilter());
      }
      if (skip) {
        document.setIndex(i);
        continue;
      }

      document.setIndex(i);

      if (instructions[i].getWebGet() != null) {
        extra.setName(instructions[i].getWebGet());

        ServiceInfoKeyValuePair kv = null;

        if (instructions[i].getSha384() != null) {
          kv = new ServiceInfoKeyValuePair();
          byte[] shaValue = null;
          try {
            shaValue = Hex.decodeHex(instructions[i].getSha384());
          } catch (DecoderException e) {
            throw new InternalServerErrorException(e);
          }
          kv.setKeyName(SHA_384);
          kv.setValue(Mapper.INSTANCE.writeValue(shaValue));
          state.getGlobalState().getQueue().add(kv);
        }

        if (instructions[i].getName() != null) {
          kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(NAME);
          kv.setValue(Mapper.INSTANCE.writeValue(instructions[i].getName()));
          state.getGlobalState().getQueue().add(kv);
        }

        kv = new ServiceInfoKeyValuePair();
        kv.setKeyName(URL);
        kv.setValue(Mapper.INSTANCE.writeValue(instructions[i].getWebGet()));
        state.getGlobalState().getQueue().add(kv);

        if (!state.getActiveSent()) {
          ServiceInfoKeyValuePair activePair = new ServiceInfoKeyValuePair();
          activePair.setKeyName(ACTIVE);
          activePair.setValue(Mapper.INSTANCE.writeValue(state.isActive()));
          state.setActiveSent(true);
          state.getGlobalState().getQueue().addFirst(activePair);
        }

      }
    }

  }


}
