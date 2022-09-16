// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.security.PrivateKey;
import javax.security.auth.DestroyFailedException;

/**
 * Wraps a PrivateKey as a closable resource.
 */
public class CloseableKey implements AutoCloseable {

  private final PrivateKey key;

  /**
   * Constructs a ClosableKey resource.
   *
   * @param key The key to wrap as a closeable resource.
   */
  public CloseableKey(PrivateKey key) {
    this.key = key;
  }

  /**
   * Gets the PrivateKey backed by the resource.
   *
   * @return The PrivateKey represented resource.
   */
  public PrivateKey get() {
    return key;
  }

  @Override
  public void close() throws IOException {
    if (!key.isDestroyed()) {
      try {
        key.destroy();
      } catch (DestroyFailedException e) {
        //some security providers do not implement this
      }
    }
  }
}
