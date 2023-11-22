// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.util.Objects;

import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.DevModList;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;
import org.fidoalliance.fdo.protocol.serviceinfo.FdoSys;

public class FdoSimCsrOwnerModule implements ServiceInfoModule {


  public static final String MODULE_NAME = "fdo.csr";
  public static final String ACTIVE = MODULE_NAME + ":active";
  public static final String CACERTS_REQ = MODULE_NAME + ":cacerts-req";
  public static final String CACERTS_RES = MODULE_NAME + ":cacerts-res";
  public static final String SIMPLEENROLL_REQ = MODULE_NAME + ":simpleenroll-req";
  public static final String SIMPLEENROLL_RES = MODULE_NAME + ":simpleenroll-res";
  public static final String SIMPLEREENROLL_REQ = MODULE_NAME + ":simplereenroll-req";
  public static final String SIMPLEREENROLL_RES = MODULE_NAME + ":simplereenroll-res";
  public static final String SERVERKEYGEN_REQ = MODULE_NAME + ":serverkeygen-req";
  public static final String SERVERKEYGEN_RES = MODULE_NAME + ":serverkeygen-res";
  public static final String CSRATTRS_REQ = MODULE_NAME + ":csrattrs-req";
  public static final String CSRATTRS_RES = MODULE_NAME + ":csrattrs-res";
  public static final String ERROR = MODULE_NAME + ":error";


  private final LoggerService logger = new LoggerService(FdoSimCsrOwnerModule.class);


  @Override
  public String getName() {
    return MODULE_NAME;
  }

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

            //extra.setWaiting(true);

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

      case SIMPLEENROLL_REQ:
        if (state.isActive()) {

          logger.info("CRS REQ");
          extra.setWaiting(false);
        }
        break;
      case CACERTS_REQ:
        if (state.isActive()) {

          logger.info("CA REQ");
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

    if (!state.getActiveSent()) {
      ServiceInfoKeyValuePair activePair = new ServiceInfoKeyValuePair();
      activePair.setKeyName(ACTIVE);
      activePair.setValue(Mapper.INSTANCE.writeValue(state.isActive()));
      state.setActiveSent(true);
      state.getGlobalState().getQueue().addFirst(activePair);
    }

    if (infoReady(extra)) {
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
}
