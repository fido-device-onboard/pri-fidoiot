// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

class To0OwnerSign implements ProtocolMessage {

  public static final Integer ID = 22;

  private final To0OwnerSignTo0d to0d;
  private final SignatureBlock to1d;

  public To0OwnerSign(To0OwnerSignTo0d to0d, SignatureBlock to1d) {
    this.to0d = to0d;
    this.to1d = to1d;
  }

  public To0OwnerSignTo0d getTo0d() {
    return to0d;
  }

  public SignatureBlock getTo1d() {
    return to1d;
  }

  @Override
  public MessageType getType() {
    return MessageType.TO0_OWNER_SIGN;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }
}
