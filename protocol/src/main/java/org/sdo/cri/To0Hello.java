// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To0Hello implements ProtocolMessage {

  public static final Integer ID = 20;

  @Override
  public MessageType getType() {
    return MessageType.TO0_HELLO;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }
}
