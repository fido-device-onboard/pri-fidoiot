// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

/**
 * To2 Server Storage Interface.
 */
public interface To2ServerStorage extends StorageEvents {

  PrivateKey geOwnerSigningKey(PublicKey key);

  byte[] getNonce6();

  void setNonce6(byte[] nonce);

  byte[] getNonce7();

  void setNonce7(byte[] nonce);

  void setOwnerState(Composite ownerState);

  Composite getOwnerState();

  void setCipherName(String cipherName);

  String getCipherName();

  void setGuid(UUID guid);

  Composite getVoucher();

  Composite getSigInfoB(Composite sigInfoA);

  Composite getReplacementRvInfo();

  UUID getReplacementGuid();

  Composite getReplacementOwnerKey();

  void setReplacementHmac(Composite hmac);

  void prepareServiceInfo();

  Composite getNextServiceInfo();

  void setServiceInfo(Composite info, boolean isMore);

}
