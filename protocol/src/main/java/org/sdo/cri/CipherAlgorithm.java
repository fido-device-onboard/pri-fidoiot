// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

enum CipherAlgorithm {
  AES128("AES"),
  AES256("AES");

  private final String shortName;

  private CipherAlgorithm(String shortName) {
    this.shortName = shortName;
  }

  public String getShortName() {
    return shortName;
  }
}
