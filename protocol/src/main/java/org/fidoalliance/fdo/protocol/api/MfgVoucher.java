// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.security.cert.Certificate;
import java.util.List;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpUtils;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.PemLoader;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.entity.ManufacturedVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;

/**
 * Get API for Manufacturing voucher.
 */
public class MfgVoucher extends RestApi {
  protected static final LoggerService logger = new LoggerService(MfgVoucher.class);


  @Override
  public void doPost() throws Exception {

    String serialNo = getLastSegment();
    logger.info("Manufacturing Voucher serialNo : " + serialNo);

    ManufacturedVoucher mfgVoucher = getSession().get(ManufacturedVoucher.class, serialNo);
    if (mfgVoucher == null) {
      logger.warn("Manufacturing voucher is null");
      throw new NotFoundException(serialNo);
    }
    OwnershipVoucher voucher = Mapper.INSTANCE.readValue(mfgVoucher.getData(),
        OwnershipVoucher.class);

    KeyResolver resolver = Config.getWorker(ManufacturerKeySupplier.class).get();

    List<Certificate> list = PemLoader.loadCerts(getStringBody());
    Certificate[] certs = list.stream()
        .toArray(Certificate[]::new);

    OwnershipVoucher result = VoucherUtils.extend(voucher, resolver, certs);
    byte[] data = Mapper.INSTANCE.writeValue(result);

    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(VoucherUtils.toString(data));
  }

  @Override
  public void doGet() throws Exception {

    String path = getLastSegment();
    logger.info("Manufacturing Voucher SerialNo: " + path);

    ManufacturedVoucher mfgVoucher = getSession().get(ManufacturedVoucher.class, path);
    if (mfgVoucher == null) {
      logger.warn("Mfg voucher is null");
      throw new NotFoundException(path);
    }
    String text = VoucherUtils.toString(mfgVoucher.getData());
    getResponse().setContentType(HttpUtils.HTTP_PLAIN_TEXT);
    getResponse().getWriter().print(text);
  }
}
