// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.UUID;

/**
 * PM.OwnershipVoucher.oh.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.2.3: PM.OwnershipVoucher"
 */
class OwnershipVoucherHeader implements Serializable {

  public static final Version THIS_VERSION = Version.VERSION_1_13;
  private String deviceInfo;
  private UUID uuid;
  private HashDigest hdc;
  private KeyEncoding pe;
  private PublicKey pk;
  private RendezvousInfo rendezvousInfo;

  /**
   * Constructor.
   */
  OwnershipVoucherHeader() {
    pe = KeyEncoding.NONE;
    rendezvousInfo = new RendezvousInfo();
    uuid = Uuids.buildRandomUuid();
    deviceInfo = "";
    pk = null;
    hdc = null;
  }

  /**
   * Constructor.
   */
  OwnershipVoucherHeader(
      KeyEncoding pe,
      RendezvousInfo r,
      UUID g,
      String d,
      PublicKey pk,
      HashDigest hdc) {

    this.pe = pe;
    rendezvousInfo = r;
    uuid = g;
    deviceInfo = d;
    this.pk = pk;
    this.hdc = hdc;
  }

  public String getD() {
    return deviceInfo;
  }

  public void setD(String d) {
    deviceInfo = d;
  }

  public UUID getG() {
    return uuid;
  }

  public void setG(UUID g) {
    uuid = g;
  }

  public HashDigest getHdc() {
    return hdc;
  }

  public void setHdc(HashDigest hdc) {
    this.hdc = hdc;
  }

  public KeyEncoding getPe() {
    return pe;
  }

  public void setPe(KeyEncoding pe) {
    this.pe = pe;
  }

  public PublicKey getPk() {
    return pk;
  }

  public void setPk(PublicKey pk) {
    this.pk = pk;
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
