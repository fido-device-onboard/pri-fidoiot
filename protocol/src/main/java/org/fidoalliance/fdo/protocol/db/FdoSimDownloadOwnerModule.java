// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.fidoalliance.fdo.protocol.message.ServiceInfoDocument;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;
import org.fidoalliance.fdo.protocol.serviceinfo.FdoSys;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class FdoSimDownloadOwnerModule implements ServiceInfoModule {

  public static final String MODULE_NAME = "fdo.download";
  public static final String ACTIVE = MODULE_NAME + ":active";
  public static final String NAME = MODULE_NAME + ":name";
  public static final String LENGTH = MODULE_NAME + ":length";
  public static final String SHA_384 = MODULE_NAME + ":sha-384";
  public static final String DATA = MODULE_NAME + ":data";
  public static final String DONE = MODULE_NAME + ":done";


  private final LoggerService logger = new LoggerService(FdoSimDownloadOwnerModule.class);

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

      case DONE:
        if (state.isActive()) {
          extra.setWaiting(false);
          int result = Mapper.INSTANCE.readValue(kvPair.getValue(), Integer.class);
          if (result == -1) {
            throw new InternalServerErrorException(FdoSimDownloadOwnerModule.DONE
                + " " + getName() + " Hash did not match");
          } else if (result >= 0) {
            if (extra.getFileLength().isEmpty() || extra.getFileLength().poll() != result) {
              throw new InternalServerErrorException(FdoSimDownloadOwnerModule.DONE
                  + " " + getName() + " all bytes not received");
            }
            logger.info(FdoSimDownloadOwnerModule.DONE
                + " " + extra.getName() + " file received");
          } else {
            throw new InternalServerErrorException(FdoSimDownloadOwnerModule.DONE
                + " " + getName() + " invalid value ");
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

  protected void getFile(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction)
      throws IOException {

    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(NAME);
    kv.setValue(Mapper.INSTANCE.writeValue(instruction.getFileDesc()));
    state.getGlobalState().getQueue().add(kv);
    extra.setName(instruction.getFileDesc());

    String resource = instruction.getResource();
    if (resource.startsWith("https://") || resource.startsWith("http://")) {
      getUrlFile(state, extra, instruction);
    } else {
      getDbFile(state, extra, instruction);
    }


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
      if (extra.isWaiting()) {
        break;
      }
      boolean sent = sendFunction.apply(state.getGlobalState().getQueue().peek());
      if (sent) {
        checkWaiting(extra, Objects.requireNonNull(state.getGlobalState().getQueue().poll()));
      } else {
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
      case LENGTH:
        extra.setReceived(0);
        break;
      case DATA:
        byte[] data = Mapper.INSTANCE.readValue(kv.getValue(), byte[].class);
        if (data.length == 0) {
          extra.setWaiting(true);
        }
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

  protected void getDbFile(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    String resource = instruction.getResource();
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      resource = resource.replace("$(guid)", state.getGuid().toString());

      // Query database table SYSTEM_RESOURCE for filename Key
      final SystemResource sviResource = session.get(SystemResource.class, resource);

      if (sviResource != null) {
        Blob blobData = sviResource.getData();
        int bufferCount = 1;
        try (InputStream input = blobData.getBinaryStream()) {
          MessageDigest msgDigest = MessageDigest.getInstance("SHA-384");
          int fileLength = 0;
          for (; ; ) {
            byte[] data = new byte[state.getMtu() - 26];
            int br = input.read(data);
            if (br == -1) {
              break;
            }
            ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(DATA);

            if (br < data.length) {
              byte[] temp = data;
              data = new byte[br];
              System.arraycopy(temp, 0, data, 0, br);
            }

            if (data.length > 0) {
              fileLength += data.length;
              msgDigest.update(data);
              kv.setValue(Mapper.INSTANCE.writeValue(data));
              state.getGlobalState().getQueue().add(kv);
              bufferCount = bufferCount + 1;
            }
          }

          ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(DATA);
          kv.setValue(Mapper.INSTANCE.writeValue(new byte[0])); //send empty
          state.getGlobalState().getQueue().add(kv);

          kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(SHA_384);
          kv.setValue(Mapper.INSTANCE.writeValue(msgDigest.digest()));
          //state.getGlobalState().getQueue().add(kv);
          ServiceInfoQueue queue = state.getGlobalState().getQueue();
          queue.add(queue.size() - bufferCount - 1, kv);

          kv = new ServiceInfoKeyValuePair();
          kv.setKeyName(LENGTH);
          kv.setValue(Mapper.INSTANCE.writeValue(fileLength));
          //state.getGlobalState().getQueue().add(kv);
          queue = state.getGlobalState().getQueue();
          queue.add(queue.size() - bufferCount - 1, kv);
          extra.setLength(fileLength);
          extra.getFileLength().add(fileLength);


          //kv = new ServiceInfoKeyValuePair();
          //kv.setKeyName(NAME);
          //kv.setValue(Mapper.INSTANCE.writeValue(instruction.getFileDesc()));
          //state.getGlobalState().getQueue().addFirst(kv);
          //extra.setName(instruction.getFileDesc());


        } catch (SQLException | NoSuchAlgorithmException throwables) {
          throw new InternalServerErrorException(throwables);
        }
      } else {
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
      int fileLength = 0;
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
              kv.setKeyName(DATA);

              if (br < data.length) {
                fileLength = fileLength + br;
                byte[] temp = data;
                data = new byte[br];
                System.arraycopy(temp, 0, data, 0, br);
              }
              kv.setValue(Mapper.INSTANCE.writeValue(data));
              state.getGlobalState().getQueue().add(kv);
            }
            extra.getFileLength().add(fileLength);
          }
        }
      }
    } catch (RuntimeException e) {
      logger.error("Runtime Exception:" + e.getMessage());
      throw new InternalServerErrorException(e);
    } catch (Exception e) {
      logger.error("failed to get http content" + e.getMessage());
      throw new InternalServerErrorException(e);
    }
    logger.info("http content downloaded successfully!");

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
        if (instructions[i].getFileDesc() != null) {
          getFile(state, extra, instructions[i]);
        } else {
          break;
        }
      }
    }

    if (!state.getActiveSent()) {
      ServiceInfoKeyValuePair activePair = new ServiceInfoKeyValuePair();
      activePair.setKeyName(ACTIVE);
      activePair.setValue(Mapper.INSTANCE.writeValue(state.isActive()));
      state.setActiveSent(true);
      state.getGlobalState().getQueue().addFirst(activePair);
    }

  }
}
