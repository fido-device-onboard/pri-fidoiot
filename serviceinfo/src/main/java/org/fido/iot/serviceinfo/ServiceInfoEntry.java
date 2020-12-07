// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

public class ServiceInfoEntry extends SimpleEntry<String, ServiceInfoSequence> {

  public ServiceInfoEntry(String key, ServiceInfoSequence value) {
    super(key, value);
  }

  public ServiceInfoEntry(Entry<? extends String, ? extends ServiceInfoSequence> entry) {
    super(entry);
  }
}
