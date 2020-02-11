// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To2GetNextOwnerServiceInfo implements ProtocolMessage {

  private final Integer nn;

  public To2GetNextOwnerServiceInfo(final Integer nn) {
    this.nn = nn;
  }

  public Integer getNn() {
    return nn;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO2_GET_NEXT_OWNER_SERVICE_INFO;
  }
}
