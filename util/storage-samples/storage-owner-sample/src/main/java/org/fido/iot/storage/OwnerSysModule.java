// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.storage;

import java.util.UUID;
import javax.sql.DataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.serviceinfo.DevMod;
import org.fido.iot.serviceinfo.FdoSys;
import org.fido.iot.serviceinfo.Module;

/**
 * Implements the owner side of the FDO sys Module.
 */
public class OwnerSysModule implements Module {

  private Composite state;
  private final DataSource dataSource;
  private static final int MAX_READ = 1277;


  /**
   * Constructs the OwnerSysModule.
   *
   * @param ds Datasource instance.
   */
  public OwnerSysModule(DataSource ds) {
    dataSource = ds;
    state = Composite.newArray();
  }

  @Override
  public String getName() {
    return FdoSys.NAME;
  }

  @Override
  public void prepare(UUID guid) {
    state.set(Const.FIRST_KEY, guid.toString());//GUID
    state.set(Const.SECOND_KEY, -1);//current resource index-1
    state.set(Const.THIRD_KEY, Composite.newArray());//resource list
  }

  @Override
  public void setMtu(int maxMtu) {
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

  }

  @Override
  public boolean isMore() {
    String guid = state.getAsString(Const.FIRST_KEY);
    int resIndex = state.getAsNumber(Const.SECOND_KEY).intValue();

    if (resIndex < 0) { // we don't have module info

      Composite deviceInfo = new OwnerDbManager().getDeviceInfo(dataSource, guid);
      if (deviceInfo.size() == 0) {
        return true; // return true until we have device info
      }
      //we have device info check if its active
      if (!isActive(deviceInfo)) {
        return false;// no more messages - we are not active
      }

      Composite resList =
          new OwnerDbManager().getSystemResources(dataSource, guid, deviceInfo);
      state.set(Const.THIRD_KEY, resList);

      resIndex = 0;
      state.set(Const.SECOND_KEY, resIndex);

    }
    //we have device info
    Composite resList = state.getAsComposite(Const.THIRD_KEY);
    return resIndex < resList.size();
  }

  @Override
  public boolean isDone() {
    return !isMore();
  }

  @Override
  public Composite nextMessage() {
    int resIndex = state.getAsNumber(Const.SECOND_KEY).intValue();
    Composite resList = state.getAsComposite(Const.THIRD_KEY);
    if (resIndex < 0) {
      return resList; //should be empty
    }
    return getNextMessage(resList.getAsComposite(resIndex));
  }

  private boolean isActive(Composite deviceInfo) {

    String sep = deviceInfo.getAsString(DevMod.KEY_SEP);
    String moduleList = deviceInfo.getAsString(DevMod.KEY_MODULES);
    //check name in list containing single value
    if (moduleList.equals(getName())) {
      return true;
    }
    //check name in list for first element t
    if (moduleList.startsWith(getName() + sep)) {
      return true;
    }
    //check name in list for last element
    if (moduleList.endsWith(sep + getName())) {
      return true;
    }
    //check name in list in the middle
    if (moduleList.indexOf(sep + getName() + sep) >= 0) {
      return true;
    }
    return false;
  }

  private void incrementIndex() {
    int resIndex = state.getAsNumber(Const.SECOND_KEY).intValue();
    state.set(Const.SECOND_KEY, resIndex + 1);
  }

  private Composite getNextMessage(Composite resource) {
    Composite message = Composite.newArray();
    String resId = resource.getAsString(Const.FIRST_KEY);
    String contentType = resource.getAsString(Const.SECOND_KEY);

    message.set(Const.FIRST_KEY, contentType);
    switch (contentType) {
      case FdoSys.KEY_ACTIVE: {

        byte[] content = new OwnerDbManager().getSystemResourceContent(dataSource, resId);
        if (content.length == 1 && content[0] == -11) {
          message.set(Const.SECOND_KEY, true);
        } else {
          message.set(Const.SECOND_KEY, false);
        }
        incrementIndex();
      }
      break;
      case FdoSys.KEY_FILEDESC:
        message.set(Const.SECOND_KEY,
            new OwnerDbManager().getSystemResourcesFileName(dataSource, resId));
        resource.set(Const.SECOND_KEY, FdoSys.KEY_WRITE);
        resource.set(Const.THIRD_KEY, 0);
        //don't increment the index
        //incrementIndex();
        break;
      case FdoSys.KEY_WRITE: {
        int start = resource.getAsNumber(Const.THIRD_KEY).intValue();
        byte[] data =
            new OwnerDbManager().getSystemResourceContentWithRange(dataSource,
                resId, start, start + MAX_READ);
        message.set(Const.SECOND_KEY, data);
        if (data.length < MAX_READ) {
          incrementIndex();
        } else {
          resource.set(Const.THIRD_KEY, start + data.length);
        }
      }
      break;
      case FdoSys.KEY_EXEC: {

        Composite composite = Composite
            .fromObject(new OwnerDbManager().getSystemResourceContent(dataSource, resId));

        message.set(Const.SECOND_KEY, composite);
        incrementIndex();
      }
      break;

      default:
        throw new UnsupportedOperationException();
    }

    return message;
  }

  private String getVersion() {
    return getName() + "-1";// -1 version
  }


}
