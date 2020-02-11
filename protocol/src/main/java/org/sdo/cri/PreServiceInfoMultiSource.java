// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * A producer of TO2.GetNextDeviceServiceInfo[0].psi key/value pairs.
 *
 * <p>For details on usage, see {@link ServiceInfoSource}.
 *
 * @see ServiceInfoSource
 */
@FunctionalInterface
public interface PreServiceInfoMultiSource extends ServiceInfoModule {

  List<Entry<CharSequence, CharSequence>> getPreServiceInfo(UUID id);
}
