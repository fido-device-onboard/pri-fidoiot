// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.Provider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

abstract class BouncyCastleLoader {

  private static BouncyCastleProvider myProvider = null;

  static synchronized Provider load() {
    if (null == myProvider) {
      myProvider = new BouncyCastleProvider();
    }
    return myProvider;
  }
}
