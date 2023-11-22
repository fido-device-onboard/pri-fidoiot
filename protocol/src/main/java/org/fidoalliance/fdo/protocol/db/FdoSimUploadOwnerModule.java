// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;

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


public class FdoSimUploadOwnerModule implements ServiceInfoModule {


  public static final String MODULE_NAME = "fdo.upload";
  public static final String ACTIVE = MODULE_NAME + ":active";
  public static final String NAME = MODULE_NAME + ":name";
  public static final String LENGTH = MODULE_NAME + ":length";
  public static final String SHA_384 = MODULE_NAME + ":sha-384";
  public static final String DATA = MODULE_NAME + ":data";
  public static final String NEED_SHA = MODULE_NAME + ":need-sha";

  private final LoggerService logger = new LoggerService(FdoSimUploadOwnerModule.class);

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

      case SHA_384:
        if (state.isActive()) {
          byte[] digestSent = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          extra.setWaiting(false);

          try {
            MessageDigest msgDigest = MessageDigest.getInstance("SHA-384");
            byte[] computed = msgDigest.digest(extra.getData());
            if (!java.util.Arrays.equals(computed, digestSent)) {
              throw new InternalServerErrorException(new DigestException());
            }
          } catch (NoSuchAlgorithmException e) {
            throw new InternalServerErrorException(e);
          }

        }
        break;
      case LENGTH:
        if (state.isActive()) {
          int len = Mapper.INSTANCE.readValue(kvPair.getValue(), Integer.class);
          extra.setLength(len);
          extra.setData(new byte[len]);
          extra.setReceived(0);
        }
        break;
      case DATA:
        if (state.isActive()) {
          byte[] data = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          if (data.length == 0) {
            extra.setWaiting(false);
          } else {

            System.arraycopy(data, 0, extra.getData(), extra.getReceived(), data.length);
            extra.setReceived(extra.getReceived() + data.length);
          }


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
      case NAME:
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
    if (document.getInstructions() != null) {
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

        if (instructions[i].getFetch() != null) {
          extra.setName(instructions[i].getFetch());
          extra.setChecksum(new byte[0]);
          ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(NEED_SHA);
          kv.setValue(Mapper.INSTANCE.writeValue(true));
          state.getGlobalState().getQueue().add(kv);

          kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(NAME);
          kv.setValue(Mapper.INSTANCE.writeValue(instructions[i].getFetch()));
          state.getGlobalState().getQueue().add(kv);
          extra.setName(instructions[i].getFetch());

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

}
