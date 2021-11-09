// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.storage;

import java.util.UUID;
import javax.sql.DataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;
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
  private final LoggerService logger = new LoggerService(OwnerSysModule.class);
  private int maxMtu = 0;
  private static final int STATUS_INDEX = Const.SEVENTH_KEY + 1;
  private static final int NAME_INDEX = STATUS_INDEX + 1;
  private static final int WAIT_INDEX = NAME_INDEX + 1;


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
    state.set(Const.SEVENTH_KEY, false); // has message to send
    state.set(STATUS_INDEX, Composite.newArray());//status state
    state.set(NAME_INDEX, "");//waiting filename
    state.set(WAIT_INDEX, false);//waiting
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

    String guid = state.getAsString(Const.FIRST_KEY);
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
      case FdoSys.KEY_STATUS_CB: {
        Composite status = kvPair.getAsComposite(Const.SECOND_KEY);
        Composite echo = state.getAsComposite(STATUS_INDEX);

        if (echo.size() > 0) {
          echo.set(Const.FIRST_KEY, status.get(Const.FIRST_KEY));
          echo.set(Const.SECOND_KEY, status.get(Const.SECOND_KEY));
          echo.set(Const.THIRD_KEY, status.get(Const.THIRD_KEY));
          state.set(Const.SEVENTH_KEY, true); //hasMessage
        }
        notifyStatus(guid,
            state.getAsString(NAME_INDEX),
            status.getAsBoolean(Const.FIRST_KEY),
            status.getAsNumber(Const.SECOND_KEY).intValue(),
            status.getAsNumber(Const.THIRD_KEY).intValue());
      }
      break;
      case FdoSys.KEY_DATA: {
        byte[] data = kvPair.getAsBytes(Const.SECOND_KEY);
        String fileName = state.getAsString(NAME_INDEX);
        notifyFetchData(guid, fileName, data);
      }
      break;
      case FdoSys.KEY_EOT: {

        state.set(NAME_INDEX, "");
        state.set(WAIT_INDEX, false);

        incrementIndex();
        int status = kvPair.getAsComposite(Const.SECOND_KEY).getAsNumber(Const.FIRST_KEY)
            .intValue();
        String fileName = state.getAsString(NAME_INDEX);
        notifyEot(guid, fileName, status);
      }
      break;
      default:
        break;
    }
  }

  @Override
  public boolean isMore() {
    return state.getAsBoolean(Const.FIFTH_KEY);
  }

  @Override
  public boolean hasMore() {

    //check if we are waiting for status callback
    Composite status = state.getAsComposite(STATUS_INDEX);
    if (status.size() > 0) {
      return state.getAsBoolean(Const.SEVENTH_KEY); //hasMessage
    }

    //checking if waiting for fetch results
    if (state.getAsBoolean(WAIT_INDEX)) {
      return false;
    }

    //now check if we are process resources
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

    //check if we are waiting for status callback
    Composite status = state.getAsComposite(STATUS_INDEX);
    if (status.size() > 0) {

      if (status.getAsBoolean(Const.FIRST_KEY)) {
        incrementIndex();

        state.set(STATUS_INDEX, Composite.newArray());

      }
      state.set(Const.SEVENTH_KEY, false);
      // send the waiting status message
      return Composite.newArray()
          .set(Const.FIRST_KEY, FdoSys.KEY_STATUS_CB)
          .set(Const.SECOND_KEY, status);

    }

    // normal message processing
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

  protected void incrementIndex() {
    int resIndex = state.getAsNumber(Const.SECOND_KEY).intValue();
    state.set(Const.SECOND_KEY, resIndex + 1);
  }

  protected void decrementIndex() {
    int resIndex = state.getAsNumber(Const.SECOND_KEY).intValue();
    if (resIndex > 0) {
      state.set(Const.SECOND_KEY, resIndex - 1);
    }
  }

  protected void notifyStatus(String guid, String name, boolean completed, int retCode,
      int timeout) {
    logger.info(
        FdoSys.KEY_STATUS_CB
            + " completed: " + completed
            + " return code: "
            + retCode
            + " wait time: "
            + timeout
            + " guid: "
            + guid
            + " command: " + name);
    //to replay use decrementIndex();

  }

  protected void notifyEot(String guid, String name, int status) {

    logger.info(
        FdoSys.KEY_EOT
            + " status: " + status
            + " guid: "
            + guid
            + " fetch: " + name);
  }

  protected void notifyFetchData(String guid, String name, byte[] data) {
    logger.info(
        FdoSys.KEY_DATA
            + " data: bytes of length(" + data.length + ")"
            + " guid: "
            + guid
            + " fetch: " + name);
    logger.warn(Composite.toString(data));
  }

  protected void notifyFetchStart(String guid, String name) {
    logger.warn("Do not fetch sensitive data unless overriding notifications");
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
            // remove everything and add the same resource back,
            // since this specific resource still needs to be sent back to the device
            resList.clear();
            resList.set(resIndex, resource);
          }
          state.set(Const.SEVENTH_KEY, true); //has message to send
          break;
        case FdoSys.KEY_FILEDESC:
        case FdoSys.KEY_FETCH:
        case FdoSys.KEY_WRITE:
        case FdoSys.KEY_EXEC:
        case FdoSys.KEY_EXEC_CB:
          state.set(Const.FIFTH_KEY, false); //ismore
          state.set(Const.SIXTH_KEY, false);//isdone - device not active
          state.set(Const.SEVENTH_KEY, true); //has message to send
          break;
        case FdoSys.KEY_IS_DONE:
          if (getBooleanContent(resId)) {
            state.set(Const.FIFTH_KEY, false); //ismore
            state.set(Const.SIXTH_KEY, true);//isdone - device not active
          }
          state.set(Const.SEVENTH_KEY, false); //has message to send
          incrementIndex();
          break;
        case FdoSys.KEY_IS_MORE:
          if (getBooleanContent(resId)) {
            state.set(Const.FIFTH_KEY, true); //ismore
            state.set(Const.SIXTH_KEY, false);//isdone - device not active
          } else {
            state.set(Const.FIFTH_KEY, false); //ismore
          }
          state.set(Const.SEVENTH_KEY, false); //has message to send
          incrementIndex();
          break;
        default:
          break;
      }

    } else {
      state.set(Const.FIFTH_KEY, false); //ismore false
      state.set(Const.SIXTH_KEY, true);//isdone - device not active
      state.set(Const.SEVENTH_KEY, false); //has message to send
    }

  }


  private boolean getBooleanContent(String resId) {
    byte[] content = new OwnerDbManager().getSystemResourceContent(dataSource, resId);

    if (content != null && content.length == 1 && content[0] == CBOR_TRUE) {
      return true;
    } else if (content != null && content.length == 1 && content[0] == CBOR_FALSE) {
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
      case FdoSys.KEY_EXEC_CB: {
        byte[] cmd = new OwnerDbManager().getSystemResourceContent(dataSource, resId);
        Composite composite = Composite
            .fromObject(cmd);

        message.set(Const.SECOND_KEY, composite);
        state.set(Const.SEVENTH_KEY, false); // has message to send
        state.set(STATUS_INDEX, createStatus(false, 0, 0));

        String fileName =
            new OwnerDbManager().getSystemResourcesFileName(dataSource, resId);
        state.set(NAME_INDEX, fileName);

        //don't increment until status_cb[true..]
        //incrementIndex();
      }
      break;
      case FdoSys.KEY_FETCH: {

        String fileName =
            new OwnerDbManager().getSystemResourcesFileName(dataSource, resId);
        message.set(Const.SECOND_KEY, fileName);
        state.set(NAME_INDEX, fileName);
        state.set(WAIT_INDEX, true);

        notifyFetchStart(state.getAsString(Const.FIRST_KEY), fileName);
        //don't increment until eof or error
        //incrementIndex();
      }
      break;
      default:
        throw new UnsupportedOperationException();
    }

    return message;
  }

  private Composite createStatus(boolean completed, int retCode, int timeout) {
    Composite status = Composite.newArray()
        .set(Const.FIRST_KEY, completed)
        .set(Const.SECOND_KEY, retCode)
        .set(Const.THIRD_KEY, timeout);
    return Composite.newArray()
        .set(Const.FIRST_KEY, FdoSys.KEY_STATUS_CB)
        .set(Const.SECOND_KEY, status);
  }

}
