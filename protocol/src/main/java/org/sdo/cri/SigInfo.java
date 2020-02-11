// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;

/**
 * SDO SigInfo.
 *
 * @see "SDO Protocol Specification, 1.12k, 3.2: Composite Types"
 */
class SigInfo {

  private ByteBuffer info;
  private SignatureType sgType;

  public SigInfo(SignatureType sgType, ByteBuffer info) {
    this.sgType = sgType;
    this.info = Buffers.clone(info);
  }

  public ByteBuffer getInfo() {
    return info.asReadOnlyBuffer();
  }

  public void setInfo(ByteBuffer info) {
    this.info = Buffers.clone(info);
  }

  public SignatureType getSgType() {
    return sgType;
  }

  public void setSgType(SignatureType sgType) {
    this.sgType = sgType;
  }
}
