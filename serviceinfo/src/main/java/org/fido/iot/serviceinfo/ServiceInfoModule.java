// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

public interface ServiceInfoModule {

  /**
   * Supplies an ordered sequence of ServiceInfo key/value pairs for transmission to the remote end
   * of the protocol.
   *
   * @param uuid UUID
   * @return List of key-value pairs
   */
  List<Entry<CharSequence, CharSequence>> getServiceInfo(UUID uuid);

  /**
   * Consumes received ServiceInfo data.
   *
   * @param uuid UUID
   * @param entry key-value pair of serviceinfo
   */
  void putServiceInfo(UUID uuid, Entry<CharSequence, CharSequence> entry);
}
