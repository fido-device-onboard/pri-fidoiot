// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.util.function.Function;

/**
 * Factory for log provider instances.
 */
public interface LogProviderFactory extends Function<Class<?>, LogProvider> {

}
