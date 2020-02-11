// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * DI.Done.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.3.4: DI.Done"
 */
class DiDone implements ProtocolMessage {

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.DI_DONE;
  }
}
