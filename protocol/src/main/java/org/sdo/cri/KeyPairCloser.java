// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.security.auth.DestroyFailedException;

class KeyPairCloser implements AutoCloseable {

  private final KeyPair myKeyPair;

  KeyPairCloser(KeyPair keyPair) {
    myKeyPair = keyPair;
  }

  PrivateKey getPrivate() {
    return myKeyPair.getPrivate();
  }

  PublicKey getPublic() {
    return myKeyPair.getPublic();
  }

  @Override
  public void close() {
    PrivateKey k = this.getPrivate();
    if (!(null == k || k.isDestroyed())) {
      try {
        k.destroy();
      } catch (DestroyFailedException e) {
        ; // Most security providers don't do this right, this is expected
      }
    }
  }
}
