// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To2OpNextEntry implements ProtocolMessage {

  private final SignatureBlock eni;
  private final Integer enn;

  public To2OpNextEntry(final Integer enn, final SignatureBlock eni) {
    this.enn = enn;
    this.eni = eni;
  }

  public SignatureBlock getEni() {
    return eni;
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
    return MessageType.TO2_OP_NEXT_ENTRY;
  }
}
