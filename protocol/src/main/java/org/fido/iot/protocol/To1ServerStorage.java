// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PublicKey;
import java.util.UUID;

import org.fido.iot.protocol.ondie.OnDieService;

/**
 * To1 Server Storage Interface.
 */
public interface To1ServerStorage extends StorageEvents {

  UUID getGuid();

  void setGuid(UUID guid);

  byte[] getNonce4();

  void setNonce4(byte[] nonce4);

  public Composite getSigInfoA();

  public void setSigInfoA(Composite sigInfoA);

  PublicKey getVerificationKey();

  Composite getRedirectBlob();

  OnDieService getOnDieService();
}
