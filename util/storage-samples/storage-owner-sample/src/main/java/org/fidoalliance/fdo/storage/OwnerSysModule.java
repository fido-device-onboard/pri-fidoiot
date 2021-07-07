// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.storage;

import java.util.UUID;
import javax.sql.DataSource;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.serviceinfo.DevMod;
import org.fidoalliance.fdo.serviceinfo.FdoSys;
import org.fidoalliance.fdo.serviceinfo.Module;

/**
 * Implements the owner side of the FDO sys Module.
 */
public class OwnerSysModule implements Module {

  private Composite state;
  private final DataSource dataSource;
  private static final byte CBOR_TRUE = (byte) 0xF5;
  private static final byte CBOR_FALSE = (byte) 0xF4;

  private int maxMtu = 0;

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
    state.set(Const.FOURTH_KEY, false); // is active flag
    state.set(Const.FIFTH_KEY, false);//is more flag
    state.set(Const.SIXTH_KEY, false);//is done flag
    state.set(Const.SEVENTH_KEY,false); // has message to send
  }

  @Override
  public void setMtu(int maxMtu) {
    this.maxMtu = maxMtu;
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
      case FdoSys.KEY_ACTIVE:
        if (value instanceof Boolean) {
          if (!((Boolean) value)) {
            //deactivate my removing resource list
            state.set(Const.SECOND_KEY, 0);
            state.set(Const.THIRD_KEY, Composite.newArray());//resource list
          }
        } else {
          throw new RuntimeException(new InvalidMessageException());
        }

        break;
      case FdoSys.KEY_KEEP_ALIVE:
      case FdoSys.KEY_RET_CODE:
      default:
        break;
    }
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
      if (isActive(deviceInfo)) {
        //get the list of resource
        Composite resList =
            new OwnerDbManager().getSystemResources(dataSource, guid, deviceInfo);
        state.set(Const.THIRD_KEY, resList);

        resIndex = 0;
        state.set(Const.SECOND_KEY, resIndex);

      }
    }
    checkMessage();
    return state.getAsBoolean(Const.SEVENTH_KEY); //hasMessage

  }

  @Override
  public boolean isDone() {
    return state.getAsBoolean(Const.SIXTH_KEY);//done
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

  private void checkMessage() {

    int resIndex = state.getAsNumber(Const.SECOND_KEY).intValue();
    Composite resList = state.getAsComposite(Const.THIRD_KEY);

    if (resIndex < resList.size()) {
      Composite resource = resList.getAsComposite(resIndex);
      String resId = resource.getAsString(Const.FIRST_KEY);
      String contentType = resource.getAsString(Const.SECOND_KEY);
      switch (contentType) {
        case FdoSys.KEY_ACTIVE:
          if (!getBooleanContent(resId)) {
            state.set(Const.FIFTH_KEY, false); //ismore
            state.set(Const.SIXTH_KEY, true);//isdone - device not active
            resList.clear();
          }
          state.set(Const.SEVENTH_KEY,true); //has message to send
          break;
        case FdoSys.KEY_FILEDESC:
        case FdoSys.KEY_WRITE:
        case FdoSys.KEY_EXEC:
          state.set(Const.FIFTH_KEY, false); //ismore
          state.set(Const.SIXTH_KEY, false);//isdone - device not active
          state.set(Const.SEVENTH_KEY,true); //has message to send
          break;
        case FdoSys.KEY_IS_DONE:
          if (getBooleanContent(resId)) {
            state.set(Const.FIFTH_KEY, false); //ismore
            state.set(Const.SIXTH_KEY, true);//isdone - device not active
          }
          state.set(Const.SEVENTH_KEY,false); //has message to send
          incrementIndex();
          break;
        case FdoSys.KEY_IS_MORE:
          if (getBooleanContent(resId)) {
            state.set(Const.FIFTH_KEY, true); //ismore
            state.set(Const.SIXTH_KEY, false);//isdone - device not active
          } else {
            state.set(Const.FIFTH_KEY, false); //ismore
          }
          state.set(Const.SEVENTH_KEY,false); //has message to send
          incrementIndex();
          break;
        default:
          break;
      }

    } else {
      state.set(Const.FIFTH_KEY, false); //ismore
      state.set(Const.SIXTH_KEY, true);//isdone - device not active
      state.set(Const.SEVENTH_KEY,false); //has message to send
    }

  }


  private boolean getBooleanContent(String resId) {
    byte[] content = new OwnerDbManager().getSystemResourceContent(dataSource, resId);
    if (content.length == 1 && content[0] == CBOR_TRUE) {
      return true;
    } else if (content.length == 1 && content[0] == CBOR_FALSE) {
      return false;
    }
    throw new RuntimeException(new InvalidMessageException());
  }

  private Composite getNextMessage(Composite resource) {
    Composite message = Composite.newArray();
    String resId = resource.getAsString(Const.FIRST_KEY);
    String contentType = resource.getAsString(Const.SECOND_KEY);

    message.set(Const.FIRST_KEY, contentType);
    switch (contentType) {
      case FdoSys.KEY_ACTIVE: {

        if (getBooleanContent(resId)) {
          message.set(Const.SECOND_KEY, true);
          state.set(Const.FOURTH_KEY, true);//isactive true
        } else {
          message.set(Const.SECOND_KEY, false);
          state.set(Const.FOURTH_KEY, false);//isactive
          state.set(Const.FIFTH_KEY, false);//ismore
          state.set(Const.SIXTH_KEY, true);//isdone
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
        // Adjust the chunk size based on the mtu size.
        // Account for the extra size due to key exchange encryption,
        // key size and other cbor bytes by adjusting downward by 100.
        byte[] data =
            new OwnerDbManager().getSystemResourceContentWithRange(dataSource,
                resId, start, start + maxMtu - 100);
        message.set(Const.SECOND_KEY, data);
        if (data.length < maxMtu - 100) {
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

}
