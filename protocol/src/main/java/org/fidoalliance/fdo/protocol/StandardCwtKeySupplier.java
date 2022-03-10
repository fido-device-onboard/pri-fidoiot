// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.dispatch.CwtKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;

public class StandardCwtKeySupplier implements CwtKeySupplier {

  protected static class RootConfig {
    @JsonProperty("cwt")
    private CwtConfig root;

    protected CwtConfig getRoot() {
      return root;
    }
  }

  protected static class CwtConfig {
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
