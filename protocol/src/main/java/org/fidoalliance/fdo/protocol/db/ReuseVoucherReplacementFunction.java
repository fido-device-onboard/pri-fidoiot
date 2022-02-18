package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.VoucherReplacementFunction;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;

public class ReuseVoucherReplacementFunction  implements VoucherReplacementFunction {

  @Override
  public OwnershipVoucherHeader apply(OwnershipVoucher voucher) throws IOException {
    return Mapper.INSTANCE.readValue(voucher.getHeader(),OwnershipVoucherHeader.class);
  }

}
