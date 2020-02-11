// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;

class EpidKey extends EncodedKeySpec implements PublicKey, PrivateKey {

  EpidKey(byte[] groupId) {
    super(groupId);
  }

  @Override
  public String getAlgorithm() {
    return getType().toString();
  }

  @Override
  public String getFormat() {
    return getType().toString();
  }

  KeyType getType() {
    return KeyType.EPIDV2_0;
  }
}

