// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To2Done implements ProtocolMessage {

  private final HashMac hmac;
  private final Nonce n6;

  public To2Done(final HashMac hmac, final Nonce n6) {
    this.hmac = hmac;
    this.n6 = n6;
  }

  public HashMac getHmac() {
    return hmac;
  }

  public Nonce getN6() {
    return n6;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO2_DONE;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }
}
