// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * A consumer of TO2.GetNextDeviceServiceInfo[0].psi key/value pairs.
 *
 * <p>For details on usage, see {@link ServiceInfoSource}.
 *
 * @see ServiceInfoSource
 */
@FunctionalInterface
public interface PreServiceInfoSink extends ServiceInfoModule {

  void putPreServiceInfo(PreServiceInfo psi);
}
