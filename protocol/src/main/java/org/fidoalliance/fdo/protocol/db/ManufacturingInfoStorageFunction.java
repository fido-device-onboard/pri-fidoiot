// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.api.NotFoundException;
import org.fidoalliance.fdo.protocol.dispatch.VoucherStorageFunction;
import org.fidoalliance.fdo.protocol.entity.ManufacturedVoucher;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Stores MAC Addresses into database.
 */
public class ManufacturingInfoStorageFunction {

  /**
   * Stores MAC Addresses into database.
   * @param serialNo Device serial number that is used to retrieve MAC Addresses.
   * @param macAddresses MAC addresses received from the client.
   * @throws IOException Throws exception if required mfgVoucher is null.
   */
  public void store(String serialNo, byte[] macAddresses) throws IOException {
    Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      ManufacturedVoucher mfgVoucher = session.get(ManufacturedVoucher.class, serialNo);
      if (mfgVoucher == null) {
        throw new NotFoundException(serialNo);
      }
      Transaction trans = session.beginTransaction();
      mfgVoucher.setMacAddresses(macAddresses);
      session.saveOrUpdate(mfgVoucher);
      trans.commit();
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      session.close();
    }
  }
}
