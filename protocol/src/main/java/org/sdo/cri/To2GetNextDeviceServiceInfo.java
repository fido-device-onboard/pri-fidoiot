// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To2GetNextDeviceServiceInfo implements ProtocolMessage {

  private final Integer nn;
  private final PreServiceInfo psi;

  public To2GetNextDeviceServiceInfo(final Integer nn, final PreServiceInfo psi) {
    this.nn = nn;
    this.psi = psi;
  }

  public Integer getNn() {
    return nn;
  }

  public PreServiceInfo getPsi() {
    return psi;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO2_GET_NEXT_DEVICE_SERVICE_INFO;
  }
}
