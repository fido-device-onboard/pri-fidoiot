package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.fidoalliance.fdo.protocol.Config;
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
    AnyType value = AnyType.fromObject(true);
    value.wrap();
    kv.setValue(value);
    serviceInfo.add(kv);

    //os=linux
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_OS);
    value = AnyType.fromObject(System.getProperty("os.name"));
    value.wrap();
    kv.setValue(value);
    serviceInfo.add(kv);

    //devmod:arch
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_ARCH);
    value = AnyType.fromObject(System.getProperty("os.arch"));
    value.wrap();
    kv.setValue(value);
    serviceInfo.add(kv);

    //devmod:version
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_VERSION);
    value = AnyType.fromObject(System.getProperty("os.version"));
    value.wrap();
    kv.setValue(value);
    serviceInfo.add(kv);

    //devmod:device
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_DEVICE);
    value = AnyType.fromObject("FDO-Pri-Device");
    value.wrap();
    kv.setValue(value);
    serviceInfo.add(kv);

    //devmod:sep
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_SEP);
    value = AnyType.fromObject(":");
    value.wrap();
    kv.setValue(value);
    serviceInfo.add(kv);

    //devmod:bin
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_BIN);
    value = AnyType.fromObject(System.getProperty("os.arch"));
    value.wrap();
    kv.setValue(value);
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
    value = AnyType.fromObject(Long.valueOf(moduleList.size()));
    value.wrap();
    kv.setValue(value);
    serviceInfo.add(kv);

    //devmod:modules
    for (String name : moduleList) {
      DevModList list = new DevModList();
      list.add(AnyType.fromObject(Long.valueOf(0)));
      list.add(AnyType.fromObject(Long.valueOf(1)));
      list.add(AnyType.fromObject(name));
      kv = new ServiceInfoKeyValuePair();
      kv.setKeyName(DevMod.KEY_MODULES);
      value = AnyType.fromObject(list);
      value.wrap();
      kv.setValue(value);
      serviceInfo.add(kv);
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
