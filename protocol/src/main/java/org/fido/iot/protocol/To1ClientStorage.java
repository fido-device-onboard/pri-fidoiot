// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PrivateKey;

/**
 * To1 Client Storage Interface.
 */
public interface To1ClientStorage extends StorageEvents, DeviceCredentials {

  Composite getSigInfoA();

  PrivateKey getSigningKey();

  byte[] getMaroePrefix();

  void storeSignedBlob(Composite signedBlob);
}
