// Copyright 2021 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.serviceinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.ServiceInfoEncoder;


/**
 * A Module that manages one or more sub modules.
 */
public class ModuleManager implements Module {

  public static final int DEFAULT_MTU = 1300;
  public static final char MODULE_DELIMITER = ':';

  private final List<Module> moduleList;
  private Composite state;
  private boolean isDevice;


  /**
   * Constructs a Module Manager.
   */
  public ModuleManager() {
    moduleList = new ArrayList<>();
    state = Composite.newArray();
    state.set(Const.FIRST_KEY, 0); //Mtu
  }


  @Override
  public String getName() {
    return "";
  }

  @Override
  public void prepare(UUID guid) {
    for (Module module : moduleList) {
      module.prepare(guid);
    }

    state.set(Const.SECOND_KEY, false); // previous isMore from device
    state.set(Const.THIRD_KEY, Composite.newArray()); // module states
    state.set(Const.FOURTH_KEY, Composite.newArray()); // extra message
  }

  @Override
  public void setMtu(int mtu) {
    for (Module module : moduleList) {
      module.setMtu(mtu);
    }
    state.set(Const.FIRST_KEY, mtu);
  }


  @Override
  public void setState(Composite state) {

    this.state = state;
    Composite modStates = state.getAsComposite(Const.THIRD_KEY);
    for (int i = 0; i < modStates.size(); i++) {
      moduleList.get(i).setState(modStates.getAsComposite(i));
      moduleList.get(i).setMtu(state.getAsNumber(Const.FIRST_KEY).intValue());
    }
  }

  @Override
  public Composite getState() {
    Composite modStates = Composite.newArray();
    for (int i = 0; i < moduleList.size(); i++) {
      modStates.set(i, moduleList.get(i).getState());
    }
    state.set(Const.THIRD_KEY, modStates);
    return state;
  }

  @Override
  public void setServiceInfo(Composite kvPair, boolean isMore) {
    state.set(Const.SECOND_KEY, isMore); //last is more
    if (kvPair.size() > 0) {
      String prefix = kvPair.getAsString(Const.FIRST_KEY);
      int pos = prefix.lastIndexOf(MODULE_DELIMITER);
      if (pos >= 0) {
        prefix = prefix.substring(0, pos);
      }

      for (Module module : moduleList) {
        if (module.getName().startsWith(prefix)) {
          module.setServiceInfo(kvPair, isMore);
        }
      }
    }
  }


  @Override
  public boolean isMore() {

    //second state key is the previous device isMore
    if (state.getAsBoolean(Const.SECOND_KEY)) {
      return false;
    }

    // we have an extra message
    if (state.getAsComposite(Const.FOURTH_KEY).size() > 0) {
      return false;
    }

    for (Module module : moduleList) {
      if (module.isMore()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isDone() {

    //the device has more
    if (state.getAsBoolean(Const.SECOND_KEY)) {
      return false;
    }

    // we have an extra message
    if (state.getAsComposite(Const.FOURTH_KEY).size() > 0) {
      return false;
    }

    // if device has no more and no extra message see if all modules are done
    for (Module module : moduleList) {
      if (!module.isDone()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Composite nextMessage() {

    for (Module module : moduleList) {
      if (module.isMore()) {
        Composite modResult = module.nextMessage();
        if (modResult.size() > 0) {
          return modResult;
        }
      }
    }
    return Const.EMPTY_MESSAGE;
  }


  /**
   * Get the next serviceinfo from the modules.
   * @return A Composite containing deviceserviceinfo or ownerserviceinfo.
   */
  public Composite getNextServiceInfo() {
    List<Composite> list = new ArrayList<>();
    Composite extra = state.getAsComposite(Const.FOURTH_KEY);
    int mtu = state.getAsNumber(Const.FIRST_KEY).intValue();

    if (extra.size() > 0) {
      list.add(extra);
      state.set(Const.FOURTH_KEY, Composite.newArray());
    }

    while (isMore()) {
      Composite kvPair = nextMessage();
      list.add(kvPair);

      Composite calcSvi = ServiceInfoEncoder.encodeOwnerServiceInfo(
          list, true, isDone());
      int len = calcSvi.toBytes().length;
      if (len > mtu) {
        list.remove(kvPair);
        state.set(Const.FOURTH_KEY, kvPair);//set extra
        break;
      }
      if (kvPair.size() == 0) {
        break;
      }
    }

    if (isDevice) {
      return ServiceInfoEncoder.encodeDeviceServiceInfo(
          list, isMore());
    }
    return ServiceInfoEncoder.encodeOwnerServiceInfo(
        list, isMore(), isDone());
  }

  /**
   * Adds a Module to the list to be managed.
   * @param module The module implementation to be managed.
   */
  public void addModule(Module module) {
    moduleList.add(module);
  }

  /**
   * Set the serviceinfo processing mode for the manager.
   * @param value Set to true for deviceinfo processing, otherwise ownerserviceinfo will be use.
   */
  public void setDeviceMode(boolean value) {
    this.isDevice = value;
  }

}
