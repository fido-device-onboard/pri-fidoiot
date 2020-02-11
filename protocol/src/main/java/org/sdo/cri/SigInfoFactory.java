// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.nio.ByteBuffer;
import java.security.PublicKey;

class SigInfoFactory {

  /**
   * Build an {@link SigInfo} for the provided key.
   */
  public SigInfo build(PublicKey key) {

    switch (Keys.toType(key)) {

      case ECDSA_P_256:
        return new SigInfo(SignatureType.ECDSA_P_256, ByteBuffer.allocate(0));

      case ECDSA_P_384:
        return new SigInfo(SignatureType.ECDSA_P_384, ByteBuffer.allocate(0));

      case EPIDV1_0:
        return new SigInfo(SignatureType.EPID10, toGroupId(key));

      case EPIDV1_1:
        return new SigInfo(SignatureType.EPID11, toGroupId(key));

      case EPIDV2_0:
        return new SigInfo(SignatureType.EPID20, toGroupId(key));

      default:
        throw new IllegalArgumentException(key.getAlgorithm());
    }
  }

  private ByteBuffer toGroupId(PublicKey key) {

    if (key instanceof EpidKey) {
      return ByteBuffer.wrap(key.getEncoded());

    } else {
      return ByteBuffer.allocate(0);
    }
  }
}
