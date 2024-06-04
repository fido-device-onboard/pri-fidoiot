// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.entity.ManufacturedVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;

/**
 * Get API for Manufacturing voucher.
 */
public class MacAddress extends RestApi {
  protected static final LoggerService logger = new LoggerService(MacAddress.class);


  @Override
  public void doGet() throws Exception {

    String path = getLastSegment();
    logger.info("Manufacturing Voucher SerialNo: " + path);

    ManufacturedVoucher mfgVoucher = getSession().get(ManufacturedVoucher.class, path);
    if (mfgVoucher == null) {
      logger.warn("Mfg voucher is null");
      throw new NotFoundException(path);
    }
    getResponse().getOutputStream().write(mfgVoucher.getMacAddresses());
  }
}
