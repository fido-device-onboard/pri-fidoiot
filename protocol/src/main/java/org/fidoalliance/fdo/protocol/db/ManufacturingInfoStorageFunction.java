// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.api.NotFoundException;
import org.fidoalliance.fdo.protocol.dispatch.VoucherStorageFunction;
import org.fidoalliance.fdo.protocol.entity.ManufacturedVoucher;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class ManufacturingInfoStorageFunction {


  public void store(String serialNo, byte[] endorsementKey) throws IOException {
    Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      ManufacturedVoucher mfgVoucher = session.get(ManufacturedVoucher.class, serialNo);
      if (mfgVoucher == null) {
        throw new NotFoundException(serialNo);
      }
      Transaction trans = session.beginTransaction();
      mfgVoucher.setEkData(endorsementKey);
      session.saveOrUpdate(mfgVoucher);
      trans.commit();
    } catch (NotFoundException e) {
        throw new RuntimeException(e);
    } finally {
      session.close();
    }
  }
}
