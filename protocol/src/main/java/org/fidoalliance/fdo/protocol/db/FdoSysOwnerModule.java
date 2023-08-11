// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.entity.SystemResource;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.DevModList;
import org.fidoalliance.fdo.protocol.message.EotResult;
import org.fidoalliance.fdo.protocol.message.ServiceInfoDocument;
import org.fidoalliance.fdo.protocol.message.ServiceInfoGlobalState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.message.StatusCb;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;
import org.fidoalliance.fdo.protocol.serviceinfo.FdoSys;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Implements FdoSysModule spec.
 */
public class FdoSysOwnerModule implements ServiceInfoModule {


  private final LoggerService logger = new LoggerService(FdoSysOwnerModule.class);

  @Override
  public String getName() {
    return FdoSys.NAME;
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
          if (name.equals(FdoSys.NAME)) {
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
      case FdoSys.STATUS_CB:
        if (state.isActive()) {
          StatusCb status = Mapper.INSTANCE.readValue(kvPair.getValue(), StatusCb.class);

          //send notification of status
          ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(FdoSys.STATUS_CB);
          kv.setValue(Mapper.INSTANCE.writeValue(status));
          state.getGlobalState().getQueue().add(kv);
          onStatusCb(state, extra, status);
          if (status.isCompleted()) {
            // check for error
            if (status.getRetCode() != 0) {
              throw new InternalServerErrorException("Exec_cb status returned failure.");
            }
            extra.setWaiting(false);
            state.getGlobalState().getQueue().addAll(state.getGlobalState().getWaitQueue());
            state.getGlobalState().setWaitQueue(new ServiceInfoQueue());
          }
        }
        break;
      case FdoSys.DATA:
        if (state.isActive()) {
          byte[] data = Mapper.INSTANCE.readValue(kvPair.getValue(), byte[].class);
          onFetch(state, extra, data);
        }
        break;
      case FdoSys.EOT:
        if (state.isActive()) {
          extra.setWaiting(false);
          state.getGlobalState().setQueue(state.getGlobalState().getWaitQueue());
          state.getGlobalState().setWaitQueue(new ServiceInfoQueue());
          EotResult result = Mapper.INSTANCE.readValue(kvPair.getValue(), EotResult.class);
          onEot(state, extra, result);
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

    while (state.getGlobalState().getQueue().size() > 0) {
      boolean sent = sendFunction.apply(state.getGlobalState().getQueue().peek());
      if (sent) {
        checkWaiting(state.getGlobalState(), extra,
            Objects.requireNonNull(state.getGlobalState().getQueue().poll()));
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

  protected void checkWaiting(ServiceInfoGlobalState globalState, FdoSysModuleExtra extra,
      ServiceInfoKeyValuePair kv) {

    switch (kv.getKey()) {
      case FdoSys.EXEC_CB:
      case FdoSys.FETCH:
        extra.setWaiting(true);
        globalState.setWaitQueue(globalState.getQueue());
        globalState.setQueue(new ServiceInfoQueue());
        break;
      default:
        break;
    }
  }

  protected boolean checkProvider(FdoSysInstruction instruction) {
    if (instruction.getModule() != null) {
      return instruction.getModule().equals(getName());
    }
    return true;
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

  protected void onStatusCb(ServiceInfoModuleState state, FdoSysModuleExtra extra,
      StatusCb status) throws IOException {
    logger.info("status_cb completed " + status.isCompleted() + " retcode "
        + status.getRetCode() + " timeout " + status.getTimeout());
  }

  protected void onFetch(ServiceInfoModuleState state, FdoSysModuleExtra extra,
      byte[] data) throws IOException {

    logger.warn(new String(data, StandardCharsets.US_ASCII));
  }

  protected void onEot(ServiceInfoModuleState state, FdoSysModuleExtra extra, EotResult result)
      throws IOException {
    logger.info("EOT:resultCode " + result.getResult());
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
      if (instructions[i].getFileDesc() != null) {
        getFile(state, extra, instructions[i]);
      } else if (instructions[i].getExecArgs() != null) {
        getExec(state, extra, instructions[i]);
      } else if (instructions[i].getExecCbArgs() != null) {
        getExecCb(state, extra, instructions[i]);
      } else if (instructions[i].getFetch() != null) {
        getFetch(state, extra, instructions[i]);
      } else {
        break;
      }
    }



    if (!state.getActiveSent()) {
      ServiceInfoKeyValuePair activePair = new ServiceInfoKeyValuePair();
      activePair.setKeyName(FdoSys.ACTIVE);
      activePair.setValue(Mapper.INSTANCE.writeValue(state.isActive()));
      state.setActiveSent(true);
      state.getGlobalState().getQueue().addFirst(activePair);
    }
  }

  protected void getExec(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(FdoSys.EXEC);
    kv.setValue(Mapper.INSTANCE.writeValue(instruction.getExecArgs()));
    state.getGlobalState().getQueue().add(kv);
  }

  protected void getExecCb(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(FdoSys.EXEC_CB);
    kv.setValue(Mapper.INSTANCE.writeValue(instruction.getExecCbArgs()));
    state.getGlobalState().getQueue().add(kv);
  }

  protected void getFetch(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(FdoSys.FETCH);
    kv.setValue(Mapper.INSTANCE.writeValue(instruction.getFetch()));
    state.getGlobalState().getQueue().add(kv);
  }

  protected void getDbFile(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    String resource = instruction.getResource();
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      Transaction trans = session.beginTransaction();
      resource = resource.replace("$(guid)", state.getGuid().toString());

      // Query database table SYSTEM_RESOURCE for filename Key
      SystemResource sviResource = session.get(SystemResource.class, resource);

      if (sviResource != null) {
        Blob blobData = sviResource.getData();
        try (InputStream input = blobData.getBinaryStream()) {
          for (; ; ) {
            byte[] data = new byte[state.getMtu() - 26];
            int br = input.read(data);
            if (br == -1) {
              break;
            }
            ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(FdoSys.WRITE);

            if (br < data.length) {
              byte[] temp = data;
              data = new byte[br];
              System.arraycopy(temp, 0, data, 0, br);
            }
            kv.setValue(Mapper.INSTANCE.writeValue(data));
            state.getGlobalState().getQueue().add(kv);
          }
        } catch (SQLException throwables) {
          logger.error("SQL Exception " + throwables.getMessage());
          throw new InternalServerErrorException(throwables);
        }
      } else {
        logger.error("SVI resource missing");
        throw new InternalServerErrorException("svi resource missing " + resource);
      }
      trans.commit();

    } finally {
      session.close();
    }

  }

  protected void getUrlFile(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    String resource = instruction.getResource();
    resource = resource.replace("$(guid)", state.getGuid().toString());

    try (CloseableHttpClient httpClient = Config.getWorker(ServiceInfoHttpClientSupplier.class)
        .get()) {

      logger.info("HTTP(S) GET: " + resource);
      HttpGet httpRequest = new HttpGet(resource);
      try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
        logger.info(httpResponse.getStatusLine().toString());
        if (httpResponse.getStatusLine().getStatusCode() != 200) {
          throw new InternalServerErrorException(httpResponse.getStatusLine().toString());
        }
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
          logger.info("content length is " + entity.getContentLength());

          try (InputStream input = entity.getContent()) {
            logger.info("reading data");
            for (; ; ) {
              byte[] data = new byte[state.getMtu() - 26];
              int br = input.read(data);
              if (br == -1) {
                break;
              }
              ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
              kv.setKeyName(FdoSys.WRITE);

              if (br < data.length) {
                byte[] temp = data;
                data = new byte[br];
                System.arraycopy(temp, 0, data, 0, br);
              }
              kv.setValue(Mapper.INSTANCE.writeValue(data));
              state.getGlobalState().getQueue().add(kv);
            }
          }
        }
      }
    } catch (RuntimeException e) {
      logger.error("Runtime Exception" + e.getMessage());
      throw new InternalServerErrorException(e);
    } catch (Exception e) {
      logger.error("failed to get http content" + e.getMessage());
      throw new InternalServerErrorException(e);
    }
    logger.info("http content downloaded successfully!");

  }

  protected void getFile(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction)
      throws IOException {

    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(FdoSys.FILEDESC);
    kv.setValue(Mapper.INSTANCE.writeValue(instruction.getFileDesc()));
    state.getGlobalState().getQueue().add(kv);

    String resource = instruction.getResource();
    if (resource.startsWith("https://") || resource.startsWith("http://")) {
      getUrlFile(state, extra, instruction);
    } else {
      getDbFile(state, extra, instruction);
    }
  }
}
