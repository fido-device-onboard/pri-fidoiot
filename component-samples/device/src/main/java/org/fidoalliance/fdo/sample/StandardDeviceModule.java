// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.DevModList;
import org.fidoalliance.fdo.protocol.message.ServiceInfo;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;

public class StandardDeviceModule implements ServiceInfoModule {
  private static final LoggerService logger = new LoggerService(StandardDeviceModule.class);

  private final List<String> moduleNames = new ArrayList<>();
  private final List<String> unknownModules = new ArrayList<>();
  private final ServiceInfoQueue queue = new ServiceInfoQueue();


  @Override
  public String getName() {
    return DevMod.NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {

    queue.clear();
    unknownModules.clear();
    moduleNames.clear();

    //active=true (required)
    ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_ACTIVE);
    kv.setValue(Mapper.INSTANCE.writeValue(true));
    queue.add(kv);

    //os=linux
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_OS);
    kv.setValue(Mapper.INSTANCE.writeValue(System.getProperty("os.name")));
    queue.add(kv);

    //devmod:arch
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_ARCH);
    kv.setValue(Mapper.INSTANCE.writeValue(System.getProperty("os.arch")));
    queue.add(kv);

    //devmod:version
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_VERSION);
    kv.setValue(Mapper.INSTANCE.writeValue(System.getProperty("os.version")));
    queue.add(kv);

    //devmod:device
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_DEVICE);
    kv.setValue(Mapper.INSTANCE.writeValue("FDO-Pri-Device"));
    queue.add(kv);

    //devmod:sep
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_SEP);
    kv.setValue(Mapper.INSTANCE.writeValue(":"));
    queue.add(kv);

    //devmod:bin
    kv = new ServiceInfoKeyValuePair();
    kv.setKeyName(DevMod.KEY_BIN);
    kv.setValue(Mapper.INSTANCE.writeValue(System.getProperty("os.arch")));
    queue.add(kv);

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
    queue.add(kv);

    //devmod:modules
    for (String name : moduleList) {
      DevModList list = new DevModList();

      list.setIndex(0);
      list.setCount(1);
      list.setModulesNames(new String[]{name});
      kv = new ServiceInfoKeyValuePair();
      kv.setKeyName(DevMod.KEY_MODULES);
      kv.setValue(Mapper.INSTANCE.writeValue(list));
      queue.add(kv);
      moduleNames.add(name);
    }
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {

    // get the module name
    String moduleName = kvPair.getKey();
    int pos = moduleName.indexOf(':');
    if (pos > 0) {
      moduleName = moduleName.substring(0, pos);
    }
    //check if module is in the known list
    if (!moduleNames.contains(moduleName)) {
      //check if the module is in the unknown list
      if (!unknownModules.contains(moduleName)) {
        unknownModules.add(moduleName);
        ServiceInfoKeyValuePair kv = new ServiceInfoKeyValuePair();
        kv.setKeyName(moduleName + ":active");
        kv.setValue(Mapper.INSTANCE.writeValue(false));
        queue.add(kv);
        state.setDone(false);
      }
    }

  }

  @Override
  public void keepAlive() throws IOException {

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

    if (queue.size() == 0) {
      logger.info("Completed DeviceServiceInfo messages");
      state.setDone(true);
    }

  }


}
