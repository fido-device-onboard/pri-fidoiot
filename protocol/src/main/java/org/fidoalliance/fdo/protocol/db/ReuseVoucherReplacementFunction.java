// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.VoucherReplacementFunction;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;

public class ReuseVoucherReplacementFunction implements VoucherReplacementFunction {

  private static final LoggerService logger =
      new LoggerService(ReuseVoucherReplacementFunction.class);

  /**
   * Worker constructor.
   */
  public ReuseVoucherReplacementFunction() {
    logger.warn("Credential reuse enabled - use only for testing.");
  }

  @Override
  public OwnershipVoucherHeader apply(OwnershipVoucher voucher) throws IOException {
    return Mapper.INSTANCE.readValue(voucher.getHeader(), OwnershipVoucherHeader.class);
  }

}
