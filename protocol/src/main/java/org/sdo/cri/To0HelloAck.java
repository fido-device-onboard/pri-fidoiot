// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To0HelloAck implements ProtocolMessage {

  public static final Integer ID = 21;

  private Nonce n3;

  public To0HelloAck(Nonce n3) {
    setN3(n3);
  }

  public Nonce getN3() {
    return n3;
  }

  private void setN3(Nonce n3) {
    this.n3 = n3;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO0_HELLO_ACK;
  }
}
