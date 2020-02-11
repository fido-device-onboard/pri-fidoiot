// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To2GetOpNextEntry implements ProtocolMessage {

  private final Integer enn;

  public To2GetOpNextEntry(final Integer enn) {
    this.enn = enn;
  }

  public Integer getEnn() {
    return enn;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO2_GET_OP_NEXT_ENTRY;
  }
}
