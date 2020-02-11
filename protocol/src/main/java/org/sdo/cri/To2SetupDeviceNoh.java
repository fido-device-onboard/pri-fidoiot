// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.UUID;

class To2SetupDeviceNoh {

  private UUID g3;
  private Nonce n7;
  private RendezvousInfo r3;

  /**
   * Constructor.
   */
  public To2SetupDeviceNoh(RendezvousInfo r3, UUID g3, Nonce n7) {
    this.r3 = r3;
    this.g3 = g3;
    this.n7 = n7;
  }

  public UUID getG3() {
    return g3;
  }

  public void setG3(UUID g3) {
    this.g3 = g3;
  }

  public Nonce getN7() {
    return n7;
  }

  public void setN7(Nonce n7) {
    this.n7 = n7;
  }

  public RendezvousInfo getR3() {
    return r3;
  }

  public void setR3(RendezvousInfo r3) {
    this.r3 = r3;
  }
}
