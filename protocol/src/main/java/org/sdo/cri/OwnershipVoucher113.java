// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.util.List;
import java.util.UUID;

/**
 * PM.OwnershipVoucher113.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.2.3: PM.OwnershipVoucher113"
 */
class OwnershipVoucher113 implements OwnershipVoucher, Serializable {

  private final CertPath myDc;
  private final List<SignatureBlock> myEn;
  private final HashMac myHmac;
  private final OwnershipVoucherHeader myOh;

  /**
   * Constructor.
   */
  OwnershipVoucher113(
      OwnershipVoucherHeader oh, HashMac hmac, CertPath dc, List<SignatureBlock> en) {

    this.myOh = oh;
    this.myHmac = hmac;
    this.myDc = dc;
    this.myEn = en;
  }

  /**
   * Return the public key of this voucher's current owner.
   */
  PublicKey getCurrentOwnerKey() throws IOException {
    if (getEn().isEmpty()) {
      return getOh().getPk();
    } else {
      final SignatureBlock sg = getEn().get(getEn().size() - 1);
      final OwnershipVoucherEntry entry =
          new OwnershipVoucherEntryCodec.Decoder().decode(CharBuffer.wrap(sg.getBo()));
      return entry.getPk();
    }
  }

  public CertPath getDc() {
    return myDc;
  }

  public List<SignatureBlock> getEn() {
    return myEn;
  }

  public HashMac getHmac() {
    return myHmac;
  }

  public OwnershipVoucherHeader getOh() {
    return myOh;
  }

  @Override
  public String toString() {
    StringWriter w = new StringWriter();
    try {
      new OwnershipVoucherCodec.OwnershipProxyEncoder().encode(w, this);
      return w.toString();
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  @Override
  public UUID getUuid() {
    return (null != myOh ? myOh.getG() : null);
  }
}
