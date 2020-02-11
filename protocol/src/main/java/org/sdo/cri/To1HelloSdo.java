// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.UUID;

/**
 * TO1.HelloSDO.
 */
class To1HelloSdo implements ProtocolMessage {

  private SigInfo ea;
  private UUID g2;

  public To1HelloSdo(UUID g2, SigInfo ea) {
    this.g2 = g2;
    this.ea = ea;
  }

  public SigInfo getEa() {
    return ea;
  }

  public UUID getG2() {
    return g2;
  }

  public void setEa(SigInfo ea) {
    this.ea = ea;
  }

  public void setG2(UUID g2) {
    this.g2 = g2;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO1_HELLO_SDO;
  }
}
