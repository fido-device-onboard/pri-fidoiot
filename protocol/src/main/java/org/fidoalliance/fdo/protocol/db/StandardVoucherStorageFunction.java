// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.security.PublicKey;
import java.sql.Blob;
import java.util.Date;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.AlgorithmFinder;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.VoucherUtils;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.dispatch.ManufacturerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.OwnerKeySupplier;
import org.fidoalliance.fdo.protocol.dispatch.VoucherStorageFunction;
import org.fidoalliance.fdo.protocol.entity.ManufacturedVoucher;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardVoucherStorageFunction implements VoucherStorageFunction {

  @Override
  public UUID apply(String serialNo, OwnershipVoucher ownershipVoucher) throws IOException {
    Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      ManufacturedVoucher mfgVoucher = new ManufacturedVoucher();
      mfgVoucher.setSerialNo(serialNo);

      mfgVoucher.setCreatedOn(new Date(System.currentTimeMillis()));

      byte[] data = Mapper.INSTANCE.writeValue(ownershipVoucher);

      Transaction trans = session.beginTransaction();
      mfgVoucher.setData(data);
      session.saveOrUpdate(mfgVoucher);
      trans.commit();

      OwnershipVoucherHeader header =
          Mapper.INSTANCE.readValue(ownershipVoucher.getHeader(), OwnershipVoucherHeader.class);

      return header.getGuid().toUuid();
    } finally {
      session.close();
    }

  }
}
