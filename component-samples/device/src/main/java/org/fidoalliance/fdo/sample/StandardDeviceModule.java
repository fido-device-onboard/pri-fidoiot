package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.DevModList;
import org.fidoalliance.fdo.protocol.message.ServiceInfo;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;

public class StandardDeviceModule implements ServiceInfoModule {

  @Override
  public String getName() {
    return DevMod.NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {

    ServiceInfo serviceInfo = new ServiceInfo();

    //active=true (required)
    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_ACTIVE);
    kv.setValue(Mapper.INSTANCE.writeValue(true));
    serviceInfo.add(kv);

    //os=linux
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_OS);
    kv.setValue(Mapper.INSTANCE.writeValue(System.getProperty("os.name")));
    serviceInfo.add(kv);

    //devmod:arch
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_ARCH);
    kv.setValue(Mapper.INSTANCE.writeValue(System.getProperty("os.arch")));

    serviceInfo.add(kv);

    //devmod:version
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_VERSION);
    kv.setValue(Mapper.INSTANCE.writeValue(System.getProperty("os.version")));
    serviceInfo.add(kv);

    //devmod:device
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_DEVICE);
    kv.setValue(Mapper.INSTANCE.writeValue("FDO-Pri-Device"));
    serviceInfo.add(kv);

    //devmod:sep
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_SEP);
    kv.setValue(Mapper.INSTANCE.writeValue(":"));
    serviceInfo.add(kv);

    //devmod:bin
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_BIN);
    kv.setValue(Mapper.INSTANCE.writeValue(System.getProperty("os.arch")));

    serviceInfo.add(kv);

    //build module list
    List<Object> workers = Config.getWorkers();

    List<String> moduleList = new ArrayList<>();
    for (Object worker : workers) {
      //build the initial state for all modules
      if (worker instanceof ServiceInfoModule) {
        ServiceInfoModule module = (ServiceInfoModule) worker;
        moduleList.add(module.getName());
      }
    }

    //devmod:nummodules
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_NUMMODULES);

    kv.setValue(Mapper.INSTANCE.writeValue(Long.valueOf(moduleList.size())));

    serviceInfo.add(kv);

    //devmod:modules
    for (String name : moduleList) {
      DevModList list = new DevModList();
      list.add(AnyType.fromObject(Long.valueOf(0)));
      list.add(AnyType.fromObject(Long.valueOf(1)));
      list.add(AnyType.fromObject(name));
      kv = new ServiceInfoKeyValuePair();
      kv.setKeyName(DevMod.KEY_MODULES);
      kv.setValue(Mapper.INSTANCE.writeValue(list));
      serviceInfo.add(kv);
      String hex = Hex.encodeHexString(kv.getValue());
      hex.length();
    }

    state.setExtra(AnyType.fromObject(serviceInfo));
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {

  }

  @Override
  public void send(ServiceInfoModuleState state, ServiceInfoSendFunction sendFunction)
      throws IOException {
    ServiceInfo serviceInfo = state.getExtra().covertValue(ServiceInfo.class);
    while (serviceInfo.size() > 0) {
      boolean sent = sendFunction.apply(serviceInfo.getFirst());
      if (sent) {
        serviceInfo.removeFirst();
      } else {
        break;
      }
    }
    if (serviceInfo.size() == 0) {
      state.setDone(true);
    }
    state.setExtra(AnyType.fromObject(serviceInfo));
  }


}
