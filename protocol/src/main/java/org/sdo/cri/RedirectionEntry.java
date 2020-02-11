// Copyright 2019 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.security.PublicKey;
import java.time.Instant;

class RedirectionEntry implements PerishableRecord {

  private final PublicKey myDevicePk;
  private final Instant myExpiresAt;
  private final String myRedirect;

  /**
   * Constructor.
   */
  RedirectionEntry(PublicKey devicePk, String redirect, Instant expiresAt) {
    this.myDevicePk = devicePk;
    this.myExpiresAt = expiresAt;
    this.myRedirect = redirect;
  }

  public PublicKey getDevicePk() {
    return myDevicePk;
  }

  public Instant getExpiresAt() {
    return myExpiresAt;
  }

  public String getRedirect() {
    return myRedirect;
  }

  @Override
  public boolean isExpired() {
    return Instant.now().isAfter(myExpiresAt);
  }
}
