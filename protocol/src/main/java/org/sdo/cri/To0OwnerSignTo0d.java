// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.time.Duration;

class To0OwnerSignTo0d {

  private Nonce n3;
  private OwnershipVoucher113 op;
  private Duration ws;

  /**
   * Constructor.
   */
  public To0OwnerSignTo0d(OwnershipVoucher113 op, Duration ws, Nonce n3) {
    this.op = op;
    this.ws = ws;
    this.n3 = n3;
  }

  public Nonce getN3() {
    return n3;
  }

  public void setN3(Nonce n3) {
    this.n3 = n3;
  }

  public OwnershipVoucher113 getOp() {
    return op;
  }

  public void setOp(OwnershipVoucher113 op) {
    this.op = op;
  }

  public Duration getWs() {
    return ws;
  }

  public void setWs(Duration ws) {
    this.ws = ws;
  }
}
