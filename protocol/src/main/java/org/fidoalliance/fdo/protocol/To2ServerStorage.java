// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

import org.fidoalliance.fdo.protocol.ondie.OnDieService;

/**
 * To2 Server Storage Interface.
 */
public interface To2ServerStorage extends StorageEvents {

  PrivateKey getOwnerSigningKey(PublicKey key);

  byte[] getNonceTo2ProveDv();

  void setNonceTo2ProveDv(byte[] nonce);

  byte[] getNonceTo2SetupDv();

  void setNonceTo2SetupDv(byte[] nonce);

  void setOwnerState(Composite ownerState);

  Composite getOwnerState();

  void setCipherName(String cipherName);

  String getCipherName();

  void setGuid(UUID guid);

  UUID getGuid();

  void storeVoucher(Composite voucher);

  Composite getVoucher();

  public Composite getSigInfoA();

  public void setSigInfoA(Composite sigInfoA);

  Composite getReplacementRvInfo();

  UUID getReplacementGuid();

  default UUID generateReplacementGuid(UUID oldUuid) {
    return UUID.randomUUID();
  }

  Composite getReplacementOwnerKey();

  void discardReplacementOwnerKey();

  byte[] getReplacementHmac();

  void setReplacementHmac(byte[] hmac);

  void prepareServiceInfo();

  Composite getNextServiceInfo();

  void setServiceInfo(Composite info, boolean isMore);

  boolean getOwnerResaleSupport();

  String getMaxDeviceServiceInfoMtuSz();

  void setMaxOwnerServiceInfoMtuSz(int mtu);

  int getMaxOwnerServiceInfoMtuSz();

  OnDieService getOnDieService();
}
