// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.time.Duration;

class To0AcceptOwner implements ProtocolMessage {

  public static final Integer ID = 25;

  private Duration ws;

  public To0AcceptOwner(Duration ws) {
    setWs(ws);
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO0_ACCEPT_OWNER;
  }

  public Duration getWs() {
    return ws;
  }

  private void setWs(Duration ws) {
    this.ws = ws;
  }
}
