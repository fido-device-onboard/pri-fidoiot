// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config.KeyStoreConfig;
import org.fidoalliance.fdo.protocol.dispatch.ReplacementKeySupplier;

public class StandardReplacementKeySupplier implements ReplacementKeySupplier {

  protected static class RootConfig {

    @JsonProperty("owner")
    private OwnerConfig root;

    protected OwnerConfig getRoot() {
      return root;
    }
  }

  protected static class OwnerConfig {

    @JsonProperty("replacement")
    private ReplacementConfig replacementConfig;

    protected ReplacementConfig getReplacementConfig() {
      return replacementConfig;
    }
  }

  protected static class ReplacementConfig {

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
    keyResolver.load(config.getRoot().getReplacementConfig().getKeyStoreConfig());
    return keyResolver;
  }
}
