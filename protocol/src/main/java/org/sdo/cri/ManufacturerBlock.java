// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * PM.CredMfg.
 *
 * @see "SDO Protocol Specification, 1.13a"
 */
class ManufacturerBlock {

  private String deviceInfo = "";

  public ManufacturerBlock() {
  }

  public ManufacturerBlock(String d) {
    deviceInfo = d;
  }

  public ManufacturerBlock(ManufacturerBlock that) {

    this.deviceInfo = that.getD();
  }

  public String getD() {
    return deviceInfo;
  }

  public void setD(String d) {
    deviceInfo = d;
  }
}
