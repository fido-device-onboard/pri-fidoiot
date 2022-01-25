package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.VoucherReplacementFunction;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;

public class StandardVoucherReplacementFunction implements VoucherReplacementFunction {

  @Override
  public OwnershipVoucherHeader apply(OwnershipVoucher voucher) throws IOException {

    AnyType headerTag = voucher.getHeader();
    OwnershipVoucherHeader header = headerTag.unwrap(OwnershipVoucherHeader.class);

    OwnershipVoucherHeader replaceHeader = new OwnershipVoucherHeader();
    OwnerPublicKey ownerPublicKey = header.getPublicKey();

    replaceHeader.setDeviceInfo(header.getDeviceInfo());
    replaceHeader.setVersion(header.getVersion());
    replaceHeader.setCertHash(header.getCertHash());

    //todo: get from database
    replaceHeader.setGuid(Guid.fromRandomUUID());
    replaceHeader.setRendezvousInfo(header.getRendezvousInfo());
    replaceHeader.setPublicKey(ownerPublicKey);


    return replaceHeader;
  }
}
