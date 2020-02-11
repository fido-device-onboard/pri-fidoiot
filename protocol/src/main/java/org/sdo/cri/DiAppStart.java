// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * DI.AppStart.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.3.1: DI.AppStart"
 */
class DiAppStart implements ProtocolMessage {

  private String deviceMark;

  public DiAppStart(String m) {
    deviceMark = m;
  }

  public String getM() {
    return deviceMark;
  }

  public void setM(String m) {
    deviceMark = m;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.DI_APP_START;
  }
}
