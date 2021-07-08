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
  public static final String ACTIVE_VAR = "active";

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
    state.set(Const.FIFTH_KEY, Composite.newArray());//unknown module list
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

      boolean moduleKnown = false;
      for (Module module : moduleList) {
        if (module.getName().startsWith(prefix)) {
          moduleKnown = true;
          module.setServiceInfo(kvPair, isMore);
        }
      }

      if (isDevice && !moduleKnown) {
        // check if exists in unknown module list
        Composite unkList = state.getAsComposite(Const.FIFTH_KEY);
        boolean found = false;
        for (int i = 0; i < unkList.size(); i++) {
          Composite unkEntry = unkList.getAsComposite(i);
          if (unkEntry.getAsString(Const.FIRST_KEY).equals(prefix)) {
            found = true;
            break;
          }
        }

        //if not found add to unknown module list
        if (!found) {
          unkList.set(unkList.size(),
              Composite.newArray().set(Const.FIRST_KEY,
                  prefix + MODULE_DELIMITER + ACTIVE_VAR).set(Const.SECOND_KEY, false));
        }
      }
    }
  }

  @Override
  public boolean isMore() {
    for (Module module : moduleList) {
      if (module.isMore()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasMore() {

    //second state key is the previous device isMore
    if (state.getAsBoolean(Const.SECOND_KEY)) {
      return false;
    }

    // we have an extra message
    if (state.getAsComposite(Const.FOURTH_KEY).size() > 0) {
      return true;
    }

    // we have unknown response to send
    if (state.getAsComposite(Const.FIFTH_KEY).size() > 0) {
      Composite unkList = state.getAsComposite(Const.FIFTH_KEY);
      for (int i = 0; i < unkList.size(); i++) {
        Composite unkEntry = unkList.getAsComposite(i);
        if (!unkEntry.getAsBoolean(Const.SECOND_KEY)) {
          return true;
        }
      }
    }

    for (Module module : moduleList) {
      if (module.hasMore()) {
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

    Composite unkList = state.getAsComposite(Const.FIFTH_KEY);
    for (int i = 0; i < unkList.size(); i++) {
      Composite unkEntry = unkList.getAsComposite(i);
      if (!unkEntry.getAsBoolean(Const.SECOND_KEY)) {
        String moduleName = unkEntry.getAsString(Const.FIRST_KEY);
        unkEntry.set(Const.SECOND_KEY, true);//message has been sent
        return Composite.newArray().set(Const.FIRST_KEY, moduleName)
            .set(Const.SECOND_KEY, false);
      }
    }

    for (Module module : moduleList) {
      if (module.hasMore()) {
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
   *
   * @return A Composite containing deviceserviceinfo or ownerserviceinfo.
   */
  public Composite getNextServiceInfo() {
    List<Composite> list = new ArrayList<>();
    Composite extra = state.getAsComposite(Const.FOURTH_KEY);
    int mtu = state.getAsNumber(Const.FIRST_KEY).intValue();

    if (extra.size() > 0) {
      list.add(extra);
      state.set(Const.FOURTH_KEY, Composite.newArray()); //reset extra
    }

    while (hasMore()) {
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
   *
   * @param module The module implementation to be managed.
   */
  public void addModule(Module module) {
    moduleList.add(module);
  }

  /**
   * Set the serviceinfo processing mode for the manager.
   *
   * @param value Set to true for deviceinfo processing, otherwise ownerserviceinfo will be use.
   */
  public void setDeviceMode(boolean value) {
    this.isDevice = value;
  }

}
