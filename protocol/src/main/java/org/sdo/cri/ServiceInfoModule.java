// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * A marker interface used to identify objects which implement one or more
 * of the service info source and sink interfaces.
 *
 * <p>This extra bit of type information allows configuration frameworks to identify
 * service info modules as something more precise than just 'Object'.
 */
public interface ServiceInfoModule {
}
