// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

/**
 * A responder for the SDO "SigInfo" challenges in TO1.HelloSDO and TO2.HelloDevice.
 *
 * <p>Produces the SigInfo eB response to the given SigInfo eA.
 */
class SigInfoResponder {

  private final EpidLib epidLib;

  public SigInfoResponder(EpidLib epidLib) {
    this.epidLib = epidLib;
  }

  /**
   * Respond to the eA SigInfo challenge.
   */
  public SigInfo apply(SigInfo ea) throws InterruptedException, IOException, TimeoutException {

    switch (ea.getSgType()) {

      case ECDSA_P_256:
      case ECDSA_P_384:
        // EC types use an empty EPIDInfo
        return new SigInfo(ea.getSgType(), ByteBuffer.allocate(0));

      case EPID10:
      case EPID11: {
        return new SigInfo(
            ea.getSgType(),
            ByteBuffer.wrap(epidLib.getEpidInfo11_eB(Buffers.unwrap(ea.getInfo()))));
      }

      case EPID20: {
        return new SigInfo(
            ea.getSgType(),
            ByteBuffer.wrap(epidLib.getEpidInfo20_eB(Buffers.unwrap(ea.getInfo()))));
      }

      default:
        throw new UnsupportedOperationException(ea.getSgType().toString());
    }
  }
}
