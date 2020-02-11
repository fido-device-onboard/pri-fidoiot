// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.UUID;

/**
 * PM.CredOwner.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.2.1: PM.CredOwner"
 */
class OwnerBlock {

  public static final Version THIS_VERSION = Version.VERSION_1_13;
  private UUID uuid = Uuids.buildRandomUuid();
  private KeyEncoding pe = KeyEncoding.NONE;
  private HashDigest pkh = new HashDigest();
  private RendezvousInfo rendezvousInfo = new RendezvousInfo();

  public OwnerBlock() {
  }

  /**
   * Constructor.
   */
  public OwnerBlock(KeyEncoding pe, UUID g, RendezvousInfo r, HashDigest pkh) {
    this.pe = pe;
    uuid = g;
    rendezvousInfo = r;
    this.pkh = pkh;
  }

  /**
   * Constructor.
   */
  public OwnerBlock(OwnerBlock that) {
    this.pe = that.getPe();
    this.uuid = that.getG();
    this.rendezvousInfo = new RendezvousInfo(that.getR());
    this.pkh = new HashDigest(that.getPkh());
  }

  public UUID getG() {
    return uuid;
  }

  public void setG(UUID g) {
    uuid = g;
  }

  public KeyEncoding getPe() {
    return pe;
  }

  public void setPe(KeyEncoding pe) {
    this.pe = pe;
  }

  public HashDigest getPkh() {
    return pkh;
  }

  public void setPkh(HashDigest pkh) {
    this.pkh = pkh;
  }

  public Version getPv() {
    return THIS_VERSION;
  }

  public RendezvousInfo getR() {
    return rendezvousInfo;
  }

  public void setR(RendezvousInfo r) {
    rendezvousInfo = r;
  }
}
