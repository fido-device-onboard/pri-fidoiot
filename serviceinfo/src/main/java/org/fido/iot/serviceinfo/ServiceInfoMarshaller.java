// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

@FunctionalInterface
public interface ServiceInfoMarshaller {

  /**
   * Breaks sericeinfo into chunks of MTU.
   *
   * @return MTU packet
   */
  Supplier<List<Entry<CharSequence, CharSequence>>> marshal();
}
