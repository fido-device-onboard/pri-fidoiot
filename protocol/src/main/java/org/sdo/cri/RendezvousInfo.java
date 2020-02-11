// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.ArrayList;

/**
 * SDO "RendezvousInfo" type.
 *
 * <p>A rendezvous info block is an ordered list of rendezvous instructions.
 */
public class RendezvousInfo extends ArrayList<RendezvousInstr> {

  public RendezvousInfo() {
    super();
  }

  public RendezvousInfo(RendezvousInfo other) {
    super(other);
  }
}
