// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.Map.Entry;
import java.util.UUID;

/**
 * Consumes received ServiceInfo data.
 *
 * <p>For details on usage, see {@link ServiceInfoSource}.
 *
 * @see ServiceInfoSource
 */
@FunctionalInterface
public interface ServiceInfoMultiSink extends ServiceInfoModule {

  void putServiceInfo(UUID id, Entry<CharSequence, CharSequence> entry);
}
