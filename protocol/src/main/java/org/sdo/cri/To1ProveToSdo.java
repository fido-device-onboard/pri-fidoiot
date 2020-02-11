// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.Arrays;
import java.util.UUID;

class To1ProveToSdo implements ProtocolMessage {

  private final byte[] myAi;
  private final UUID myG2;
  private final Nonce myN4;

  /**
   * Constructor.
   */
  To1ProveToSdo(byte[] ai, Nonce n4, UUID g2) {
    myAi = Arrays.copyOf(ai, ai.length);
    myG2 = g2;
    myN4 = n4;
  }

  public byte[] getAi() {
    return Arrays.copyOf(myAi, myAi.length);
  }

  public UUID getG2() {
    return myG2;
  }

  public Nonce getN4() {
    return myN4;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO1_PROVE_TO_SDO;
  }
}
