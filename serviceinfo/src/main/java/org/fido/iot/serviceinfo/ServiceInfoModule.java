// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.util.List;
import java.util.UUID;

public interface ServiceInfoModule {

  /**
   * Supplies an ordered sequence of {@link ServiceInfoEntry} to {@link ServiceInfoMarshaller}.
   * Implementation must provide mechanism to fetch service info on-demand.
   *
   * @param uuid UUID
   * @return List of key-value pairs as {@link ServiceInfoEntry}
   */
  List<ServiceInfoEntry> getServiceInfo(UUID uuid);

  /**
   * Consumes received ServiceInfo data. Implementation should provide mechanism to consume the
   * received service info.
   *
   * @param uuid UUID
   * @param entry key-value pair of serviceinfo
   */
  void putServiceInfo(UUID uuid, ServiceInfoEntry entry);
}
