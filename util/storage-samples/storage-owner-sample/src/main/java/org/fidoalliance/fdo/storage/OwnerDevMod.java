// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.storage;

import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.serviceinfo.DevMod;
import org.fidoalliance.fdo.serviceinfo.Module;

/**
 * Implements the owner side of the FDO spec DevMod.
 */
public class OwnerDevMod implements Module {

  private Composite state;
  private final DataSource dataSource;
  public static final String GUID_KEY = "guid";
  private static final String RECEIVED_KEY = "received";
  private static final String MORE_KEY = "more";


  /**
   * Constructs a OwnerDevMod.
   *
   * @param ds Datasource instance.
   */
  public OwnerDevMod(DataSource ds) {
    this.dataSource = ds;
    this.state = Composite.newMap();

  }

  @Override
  public String getName() {
    return DevMod.NAME;
  }

  @Override
  public void prepare(UUID guid) {
    state.set(GUID_KEY, guid.toString());
    state.set(DevMod.KEY_MODULES, Composite.newArray());
    state.set(MORE_KEY, true); //expecting more
    //remove previous data
    new OwnerDbManager().removeDeviceInfo(dataSource, guid.toString());

  }

  @Override
  public void setMtu(int maxMtu) {
    //we are receiving only - not sending so don't need mtu
  }


  @Override
  public void setState(Composite state) {
    this.state = state;
  }


  @Override
  public Composite getState() {
    return state;
  }

  @Override
  public void setServiceInfo(Composite kvPair, boolean isMore) {

    String key = kvPair.getAsString(Const.FIRST_KEY);
    Object value = kvPair.get(Const.SECOND_KEY);

    switch (key) {

      case DevMod.KEY_ACTIVE:
      case DevMod.KEY_OS:
      case DevMod.KEY_ARCH:
      case DevMod.KEY_BIN:
      case DevMod.KEY_VERSION:
      case DevMod.KEY_DEVICE:
      case DevMod.KEY_PATHSEP:
      case DevMod.KEY_SEP:
      case DevMod.KEY_NL:
      case DevMod.KEY_TMP:
      case DevMod.KEY_DIR:
      case DevMod.KEY_PROGENV:
      case DevMod.KEY_MUDURL:
      case DevMod.KEY_NUMMODULES:
        state.set(key, value);
        break;
      case DevMod.KEY_SN:
        if (value instanceof String) {
          state.set(key, value);
        } else if (value instanceof byte[]) {
          state.set(key, Composite.toString((byte[]) value));
        }
        break;
      case DevMod.KEY_MODULES:
        processModules(kvPair.getAsComposite(Const.SECOND_KEY));
        break;
      default:
        break;
    }

    if (!isMore) {
      state.set(MORE_KEY, false);
      covertModuleList();
      new OwnerDbManager().addDeviceInfo(dataSource, state);
    }


  }

  @Override
  public boolean hasMore() {
    return false;
  }

  @Override
  public boolean isMore() {
    return false;
  }

  @Override
  public boolean isDone() {
    return state.getAsBoolean(MORE_KEY) == false;
  }

  @Override
  public Composite nextMessage() {
    return Const.EMPTY_MESSAGE;
  }

  private void covertModuleList() {
    if (state.get(DevMod.KEY_MODULES) instanceof List) {
      Composite modList = state.getAsComposite(DevMod.KEY_MODULES);
      String sep = state.getAsString(DevMod.KEY_SEP);
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < modList.size(); i++) {
        if (builder.length() > 0) {
          builder.append(sep);
        }
        builder.append(modList.getAsString(i));
      }
      state.set(DevMod.KEY_MODULES, builder.toString());

    }
  }

  private void processModules(Composite modules) {

    int maxIndex = state.getAsNumber(DevMod.KEY_NUMMODULES).intValue();

    int index = modules.getAsNumber(Const.FIRST_KEY).intValue();
    if (index < 0 || index > maxIndex) {
      throw new InvalidMessageException();
    }

    int modCount = modules.getAsNumber(Const.SECOND_KEY).intValue();

    for (int i = 0; i < modCount; i++) {
      String name = modules.getAsString(Const.THIRD_KEY + i);
      Composite modList = state.getAsComposite(DevMod.KEY_MODULES);
      modList.set(modList.size(), name);
    }
  }

}
