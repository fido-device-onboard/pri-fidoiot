// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Supplies an ordered sequence of ServiceInfo key/value pairs for transmission
 * to the remote end of the protocol.
 *
 * <p>For details on usage, see {@link ServiceInfoSource}.
 *
 * @see ServiceInfoSource
 */
@FunctionalInterface
public interface ServiceInfoMultiSource extends ServiceInfoModule {

  List<Entry<CharSequence, CharSequence>> getServiceInfo(UUID id);
}
