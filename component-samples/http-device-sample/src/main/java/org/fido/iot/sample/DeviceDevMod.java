// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.ServiceInfoEncoder;
import org.fido.iot.serviceinfo.DevMod;
import org.fido.iot.serviceinfo.Module;

/**
 * Device side implmenation of the FDO spec DevMod.
 */
public class DeviceDevMod implements Module {

  private final List<Composite> devInfoList;
  private final List<String> moduleNames;
  private int listIndex;

  /**
   * Constructs a DeviceDevMod.
   */
  public DeviceDevMod() {
    devInfoList = new ArrayList<>();
    moduleNames = new ArrayList<>();
  }

  @Override
  public String getName() {
    return DevMod.NAME;
  }

  @Override
  public void prepare(UUID guid) {

    listIndex = 0;

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_ACTIVE, true));
    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_OS, System.getProperty("os.name")));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_ARCH, System.getProperty("os.arch")));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_BIN, System.getProperty("os.arch")));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_VERSION, System.getProperty("os.version")));

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SDO reference device, using JRE ");
    stringBuilder.append(System.getProperty("java.version"));
    stringBuilder.append(" (");
    stringBuilder.append(System.getProperty("java.vendor"));
    stringBuilder.append(")");
    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_DEVICE, stringBuilder.toString()));

    //list.add(
    //   ServiceInfoEncoder.encodeValue(DevMod.KEY_SN, ""));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_PATHSEP, File.separator));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_SEP,
            Character.toString(File.pathSeparatorChar)));
    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_NL,
            System.getProperty("line.separator")));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_TMP,
            System.getProperty("java.io.tmpdir")));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_DIR,
            System.getProperty("java.io.tmpdir")));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_PROGENV, "bin:java"));

    //list.add(
    //   ServiceInfoEncoder.encodeValue(DevMod.KEY_MUDURL,
    // "https://fidoalliance.org/mud/sample/device"));

    devInfoList.add(
        ServiceInfoEncoder.encodeValue(DevMod.KEY_NUMMODULES, moduleNames.size()));

    //add all the modules
    for (int i = 0; i < moduleNames.size(); i++) {
      Composite modules = Composite.newArray()
          .set(0, i) //zero to num module index
          .set(1, 1) // the number returned
          .set(2, moduleNames.get(i)); //the module
      devInfoList.add(
          ServiceInfoEncoder.encodeValue(DevMod.KEY_MODULES, modules));
    }

  }

  @Override
  public void setMtu(int mtu) {
  }

  @Override
  public void setState(Composite state) {
  }

  @Override
  public Composite getState() {
    return Const.EMPTY_MESSAGE;
  }

  @Override
  public void setServiceInfo(Composite kvPair, boolean isMore) {
    //no service info
  }

  @Override
  public boolean isMore() {
    return listIndex < devInfoList.size();
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public Composite nextMessage() {
    return devInfoList.get(listIndex++);
  }

  /**
   * Adds a module name to the supported module list.
   * @param moduleName The name of the module.
   */
  public void addModuleName(String moduleName) {
    moduleNames.add(moduleName);
  }
}
