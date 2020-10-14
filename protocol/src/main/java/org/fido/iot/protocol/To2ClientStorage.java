// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.security.PrivateKey;

/**
 * To2 Client Storage Interface.
 */
public interface To2ClientStorage extends StorageEvents, DeviceCredentials {

  PrivateKey getSigningKey();

  Composite getSigInfoA();

  byte[] getMaroePrefix();

  String getKexSuiteName();

  String getCipherSuiteName();

  byte[] getReplacementHmacSecret(Composite newCredentials,boolean isReuse);

  void prepareServiceInfo();

  Composite getNextServiceInfo();

  void setServiceInfo(Composite info, boolean isMore, boolean isDone);

}
