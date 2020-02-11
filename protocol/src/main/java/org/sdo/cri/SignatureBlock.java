// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

/**
 * SDO Composite type 'SignatureBlock', from protocol specification 1.12j 3.2.
 */
class SignatureBlock implements Serializable {

  // The body text, signed (in its ASCII form, usually) by 'sg'.
  private final String bo;

  // The public key advertised in this block.  This is not necessarily the key which was
  // used to generate 'sg.'  Nulls are interpreted as the 'NONE' value.
  private final PublicKey pk;

  // The signature text.
  private final byte[] sg;

  /**
   * Constructor.
   */
  SignatureBlock(CharSequence bo, PublicKey pk, byte[] sg) {
    this.bo = bo.toString();
    this.pk = pk;
    this.sg = Arrays.copyOf(sg, sg.length);
  }

  @Override
  public boolean equals(Object thatObject) {

    if (!(thatObject instanceof SignatureBlock)) {
      return false;
    }

    SignatureBlock that = (SignatureBlock) thatObject;
    return Objects.equals(bo, that.bo)
        && Objects.equals(pk, that.pk)
        && Arrays.equals(sg, that.sg);
  }

  String getBo() {
    return bo;
  }

  PublicKey getPk() {
    return pk;
  }

  byte[] getSg() {
    return Arrays.copyOf(sg, sg.length);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getBo(), getPk(), getSg());
  }
}
