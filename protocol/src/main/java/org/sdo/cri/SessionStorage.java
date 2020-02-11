// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.time.Duration;

interface SessionStorage {

  Object load(Object key);

  void store(Object key, Object value);

  void store(Object key, Object value, Duration timeout);

  void remove(Object key);
}
