// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * DI.SetCredentials.
 *
 * @see "SDO Protocol Specification, 1.13a"
 */
class DiSetCredentials implements ProtocolMessage {

  private OwnershipVoucherHeader oh;

  public DiSetCredentials(OwnershipVoucherHeader oh) {
    this.oh = oh;
  }

  public OwnershipVoucherHeader getOh() {
    return oh;
  }

  public void setOh(OwnershipVoucherHeader oh) {
    this.oh = oh;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.DI_SET_CREDENTIALS;
  }
}
