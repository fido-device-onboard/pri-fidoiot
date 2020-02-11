// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * DI.SetHMAC.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.3.3: DI.SetHMAC"
 */
class DiSetHmac implements ProtocolMessage {

  private HashMac hmac;

  public DiSetHmac(HashMac hmac) {
    this.hmac = hmac;
  }

  public HashMac getHmac() {
    return hmac;
  }

  public void setHmac(HashMac hmac) {
    this.hmac = hmac;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.DI_SET_HMAC;
  }
}
