// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To1HelloSdoAck implements ProtocolMessage {

  private SigInfo eb;
  private Nonce n4;

  public To1HelloSdoAck(Nonce n4, SigInfo eb) {
    this.n4 = n4;
    this.eb = eb;
  }

  public SigInfo getEb() {
    return eb;
  }

  public void setEb(SigInfo eb) {
    this.eb = eb;
  }

  public Nonce getN4() {
    return n4;
  }

  public void setN4(Nonce n4) {
    this.n4 = n4;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO1_HELLO_SDO_ACK;
  }
}
