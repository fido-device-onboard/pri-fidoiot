// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

import org.fido.iot.protocol.ondie.OnDieService;

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

  void storeVoucher(Composite voucher);

  Composite getVoucher();

  public Composite getSigInfoA();

  public void setSigInfoA(Composite sigInfoA);

  Composite getReplacementRvInfo();

  UUID getReplacementGuid();

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
