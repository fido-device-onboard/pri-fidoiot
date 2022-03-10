// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.security.PublicKey;
import org.fidoalliance.fdo.protocol.AlgorithmFinder;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.HmacFunction;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.DeviceCredential;
import org.fidoalliance.fdo.protocol.message.Hash;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

public class StandardHmacFunction implements HmacFunction {


  @Override
  public Hash apply(DeviceCredential credential, byte[] headerTag) throws IOException {

    DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();
    HashType hmacType = HashType.HMAC_SHA384;
    if (config.getKeyType().equals(PublicKeyType.SECP256R1)) {
      OwnershipVoucherHeader header =
          Mapper.INSTANCE.readValue(headerTag, OwnershipVoucherHeader.class);
      CryptoService cs = Config.getWorker(CryptoService.class);
      PublicKey ownerKey = cs.decodeKey(header.getPublicKey());
      KeySizeType ownerSize = new AlgorithmFinder().getKeySizeType(ownerKey);
      if (ownerSize.equals(KeySizeType.SIZE_256)) {
        hmacType = HashType.HMAC_SHA256;
      }
    }

    CryptoService cs = Config.getWorker(CryptoService.class);
    credential.setHmacSecret(cs.createHmacKey(hmacType));

    return cs.hash(hmacType, credential.getHmacSecret(), headerTag);
  }
}
