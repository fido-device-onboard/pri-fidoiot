// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.PublicKey;

/**
 * PM.OwnershipVoucher113.en.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.2.3: PM.OwnershipVoucher113"
 */
class OwnershipVoucherEntry {

  private HashDigest hc;
  private HashDigest hp;
  private PublicKey pk;

  /**
   * Constructor.
   */
  public OwnershipVoucherEntry(HashDigest hp, HashDigest hc, PublicKey pk) {
    this.hp = hp;
    this.hc = hc;
    this.pk = pk;
  }

  public HashDigest getHc() {
    return hc;
  }

  public HashDigest getHp() {
    return hp;
  }

  public PublicKey getPk() {
    return pk;
  }

  public void setHc(HashDigest hc) {
    this.hc = hc;
  }

  public void setHp(HashDigest hp) {
    this.hp = hp;
  }

  public void setPk(PublicKey pk) {
    this.pk = pk;
  }
}
