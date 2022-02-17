package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.entity.SystemPackage;
import org.fidoalliance.fdo.protocol.entity.SystemResource;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.DevModList;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.message.StatusCb;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;
import org.fidoalliance.fdo.protocol.serviceinfo.FdoSys;
import org.hibernate.Session;

public class FdoSysOwnerModule implements ServiceInfoModule {


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
            ServiceInfoQueue queue = extra.getQueue();
            ServiceInfoKeyValuePair activePair = new ServiceInfoKeyValuePair();
            activePair.setKeyName(FdoSys.ACTIVE);
            activePair.setValue(Mapper.INSTANCE.writeValue(true));
            queue.add(activePair);

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
          if (status.isCompleted()) {
            extra.setWaiting(false);
          } else {
            //send notification of status
            ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(FdoSys.STATUS_CB);
            kv.setValue(Mapper.INSTANCE.writeValue(status));
            extra.getQueue().add(kv);
          }
        }
        break;
      default:
        break;
    }
    state.setExtra(AnyType.fromObject(extra));
  }

  @Override
  public void send(ServiceInfoModuleState state, ServiceInfoSendFunction sendFunction)
      throws IOException {

    FdoSysModuleExtra extra = state.getExtra().covertValue(FdoSysModuleExtra.class);


    if (!extra.isLoaded() && infoReady(extra)) {
      load(state, extra);
      extra.setLoaded(true);
    }

    ServiceInfoQueue queue = extra.getQueue();
    while (queue.size() > 0) {
      boolean sent = sendFunction.apply(queue.peek());
      if (sent) {
        checkWaiting(queue.poll());
      } else {
        break;
      }
    }
    if (queue.size() == 0) {
      state.setDone(true);
    }
    state.setExtra(AnyType.fromObject(extra));
  }

  protected void checkWaiting(ServiceInfoKeyValuePair kv) {
    switch (kv.getKey()) {
      case FdoSys.EXEC_CB:

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
    return devMap.entrySet().containsAll(filterMap.entrySet());
  }

  protected void load(ServiceInfoModuleState state, FdoSysModuleExtra extra)
      throws IOException {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      SystemPackage systemPackage =
          session.find(SystemPackage.class, Long.valueOf(1));

      if (systemPackage != null) {
        String body = systemPackage.getData().getSubString(1,
            Long.valueOf(systemPackage.getData().length()).intValue());
        FdoSysInstruction[] instructions =
            Mapper.INSTANCE.readJsonValue(body, FdoSysInstruction[].class);

        boolean skip = false;
        for (FdoSysInstruction instruction : instructions) {
          if (instruction.getFilter() != null) {
            skip = checkFilter(extra.getFilter(), instruction.getFilter());
          }
          if (skip) {
            continue;
          }

          if (instruction.getFileDesc() != null) {
            getFile(state, extra, instruction);
          } else if (instruction.getExecArgs() != null) {
            getExec(state, extra, instruction);
          } else if (instruction.getExecArgs() != null) {
            getExecCb(state, extra, instruction);
          }
        }

      }
    } catch (SQLException e) {
      throw new InternalServerErrorException(e);
    } finally {
      session.close();
    }
  }

  protected void getExec(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(FdoSys.EXEC);
    kv.setValue(Mapper.INSTANCE.writeValue(instruction.getExecArgs()));
    extra.getQueue().add(kv);
  }

  protected void getExecCb(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(FdoSys.EXEC);
    kv.setValue(Mapper.INSTANCE.writeValue(instruction.getExecArgs()));
    extra.getQueue().add(kv);
  }

  protected void getDbFile(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    String resource = instruction.getResource();
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      resource = resource.replace("$(guid)",state.getGuid().toString());

      // Query database table SYSTEM_RESOURCE for filename Key
      SystemResource sviResource = session.get(SystemResource.class, resource);

      if (sviResource != null) {
        Blob blobData = sviResource.getData();
        try (InputStream input = blobData.getBinaryStream()) {
          for (;;) {
            byte[] data = new byte[state.getMtu()-26];
            int br = input.read(data);
            if (br == -1 ) {
              break;
            }
            ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
            kv.setKeyName(FdoSys.WRITE);

            if (br < data.length) {
              byte[] temp = data;
              data = new byte[br];
              System.arraycopy(temp,0,data,0,br);
            }
            kv.setValue(Mapper.INSTANCE.writeValue(data));
            extra.getQueue().add(kv);
          }
        } catch (SQLException throwables) {
          throw new IOException(throwables);
        }
      }

    } finally {
      session.close();
    }

  }

  protected void getUrlFile(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction) throws IOException {
    String resource = instruction.getResource();
    resource = resource.replace("$(guid)",state.getGuid().toString());


    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

      HttpGet httpRequest = new HttpGet(resource);
      try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest);) {
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {

          try (InputStream input = entity.getContent()) {
            for (;;) {
              byte[] data = new byte[state.getMtu()-26];
              int br = input.read(data);
              if (br == -1 ) {
                break;
              }
              ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
              kv.setKeyName(FdoSys.WRITE);

              if (br < data.length) {
                byte[] temp = data;
                data = new byte[br];
                System.arraycopy(temp,0,data,0,br);
              }
              kv.setValue(Mapper.INSTANCE.writeValue(data));
              extra.getQueue().add(kv);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new IOException(e);
    }

  }

  protected void getFile(ServiceInfoModuleState state,
      FdoSysModuleExtra extra,
      FdoSysInstruction instruction)
      throws IOException {

    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(FdoSys.FILEDESC);
    kv.setValue(Mapper.INSTANCE.writeValue(instruction.getFileDesc()));
    extra.getQueue().add(kv);

    String resource = instruction.getResource();
    if (resource.startsWith("https://") || resource.startsWith("http://")) {
      getUrlFile(state, extra, instruction);
    } else {
      getDbFile(state,extra,instruction);
    }
  }
}
