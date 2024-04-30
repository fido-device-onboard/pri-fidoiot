// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import org.fidoalliance.fdo.protocol.*;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.entity.ManufacturedVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;

import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Get API for Manufacturing voucher.
 */
public class EkVoucher extends RestApi {
  protected static final LoggerService logger = new LoggerService(EkVoucher.class);


  @Override
  public void doGet() throws Exception {

    String path = getLastSegment();
    logger.info("Manufacturing Voucher SerialNo: " + path);

    ManufacturedVoucher mfgVoucher = getSession().get(ManufacturedVoucher.class, path);
    if (mfgVoucher == null) {
      logger.warn("Mfg voucher is null");
      throw new NotFoundException(path);
    }
    String text = Arrays.toString(mfgVoucher.getEkData());
    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(text);
  }
}
