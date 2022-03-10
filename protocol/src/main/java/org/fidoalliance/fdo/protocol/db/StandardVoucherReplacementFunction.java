// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.AlgorithmFinder;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.ReplacementKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.VoucherReplacementFunction;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;

public class StandardVoucherReplacementFunction implements VoucherReplacementFunction {

  @Override
  public OwnershipVoucherHeader apply(OwnershipVoucher voucher) throws IOException {

    byte[] headerTag = voucher.getHeader();

    OwnershipVoucherHeader header =
        Mapper.INSTANCE.readValue(headerTag, OwnershipVoucherHeader.class);

    OwnershipVoucherHeader replaceHeader = new OwnershipVoucherHeader();
    OwnerPublicKey ownerPublicKey = header.getPublicKey();

    replaceHeader.setDeviceInfo(header.getDeviceInfo());
    replaceHeader.setVersion(header.getVersion());
    replaceHeader.setCertHash(header.getCertHash());

    replaceHeader.setGuid(Guid.fromRandomUuid());
    replaceHeader.setRendezvousInfo(header.getRendezvousInfo());

    CryptoService cs = Config.getWorker(CryptoService.class);
    KeyResolver resolver = Config.getWorker(ReplacementKeySupplier.class).get();
    String alias = KeyResolver.getAlias(ownerPublicKey.getType(),
        new AlgorithmFinder().getKeySizeType(cs.decodeKey(ownerPublicKey)));
    OwnerPublicKey owner2Key = cs.encodeKey(ownerPublicKey.getType(),ownerPublicKey.getEnc(),
        resolver.getCertificateChain(alias));
    replaceHeader.setPublicKey(owner2Key);

    return replaceHeader;
  }
}
