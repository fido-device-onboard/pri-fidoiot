// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To2SetupDevice implements ProtocolMessage {

  private final SignatureBlock noh;
  private final Integer osinn;

  public To2SetupDevice(final Integer osinn, final SignatureBlock noh) {
    this.osinn = osinn;
    this.noh = noh;
  }

  public SignatureBlock getNoh() {
    return noh;
  }

  public Integer getOsinn() {
    return osinn;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO2_SETUP_DEVICE;
  }
}
