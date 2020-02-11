// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

class To2ProveDevice implements ProtocolMessage {

  private final byte[] myAi;
  private final UUID myG2;
  private final Nonce myN6;
  private final Nonce myN7;
  private final Integer myNn;
  private final byte[] myXb;

  /**
   * Constructor.
   */
  public To2ProveDevice(
      final ByteBuffer ai,
      final Nonce n6,
      final Nonce n7,
      final UUID g2,
      final Integer nn,
      final ByteBuffer xb) {

    this.myAi = new byte[ai.remaining()];
    ai.get(this.myAi);
    this.myN6 = n6;
    this.myN7 = n7;
    this.myG2 = g2;
    this.myNn = nn;
    this.myXb = new byte[xb.remaining()];
    xb.get(this.myXb);
  }

  public byte[] getAi() {
    return Arrays.copyOf(myAi, myAi.length);
  }

  public UUID getG2() {
    return myG2;
  }

  public Nonce getN6() {
    return myN6;
  }

  public Nonce getN7() {
    return myN7;
  }

  public Integer getNn() {
    return myNn;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO2_PROVE_DEVICE;
  }

  public byte[] getXb() {
    return Arrays.copyOf(myXb, myXb.length);
  }
}
