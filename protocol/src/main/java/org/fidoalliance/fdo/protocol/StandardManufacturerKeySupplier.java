// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;

public class StandardManufacturerKeySupplier implements ManufacturerKeySupplier {

  protected static class RootConfig {
    @JsonProperty("manufacturer")
    private MfgConfig config;

    protected MfgConfig getRoot() {
      return config;
    }
  }

  protected static class MfgConfig {
    @JsonProperty("keystore")
    private KeyStoreConfig storeConfig;

    protected KeyStoreConfig getKeyStoreConfig() {
      return storeConfig;
    }
  }

  protected RootConfig config = Config.getConfig(RootConfig.class);


  @Override
  public KeyResolver get() throws IOException {
    KeyResolver keyResolver = new KeyResolver();
    keyResolver.load(config.getRoot().getKeyStoreConfig());
    return keyResolver;
  }
}
