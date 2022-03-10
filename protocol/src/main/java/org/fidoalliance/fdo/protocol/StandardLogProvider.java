// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

public class StandardLogProvider implements LogProviderFactory {

  @Override
  public LogProvider apply(Class<?> clazz) {
    return new Log4jProvider(clazz);
  }
}
