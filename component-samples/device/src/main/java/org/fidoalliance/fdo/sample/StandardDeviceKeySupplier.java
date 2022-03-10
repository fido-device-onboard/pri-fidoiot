// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.dispatch.DeviceKeySupplier;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

public class StandardDeviceKeySupplier implements DeviceKeySupplier {


  @Override
  public KeyResolver get() throws IOException {
    DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();

    KeyResolver keyResolver = new KeyResolver();
    keyResolver.load(config.getKeyStoreConfig());

    //the key resolver will only have the selected key
    PublicKeyType keyType = config.getKeyType();
    KeySizeType sizeType;
    switch (keyType) {
      case SECP256R1:
        sizeType = KeySizeType.SIZE_256;
        break;
      case SECP384R1:
        sizeType = KeySizeType.SIZE_384;
        break;
      default:
        throw new InternalServerErrorException(new IllegalArgumentException());
    }
    String alias = KeyResolver.getAlias(keyType, sizeType);
    keyResolver.setAlias(alias);
    return keyResolver;
  }
}
