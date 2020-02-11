// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To2OwnerServiceInfo implements ProtocolMessage {

  private final Integer nn;
  private final ServiceInfo sv;

  public To2OwnerServiceInfo(final Integer nn, final ServiceInfo sv) {
    this.nn = nn;
    this.sv = sv;
  }

  public Integer getNn() {
    return nn;
  }

  public ServiceInfo getSv() {
    return sv;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO2_OWNER_SERVICE_INFO;
  }
}
