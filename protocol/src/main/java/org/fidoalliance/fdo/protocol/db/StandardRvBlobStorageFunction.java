// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.RvBlobStorageFunction;
import org.fidoalliance.fdo.protocol.entity.RvRedirect;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To2RedirectEntry;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardRvBlobStorageFunction implements RvBlobStorageFunction {

  @Override
  public Long apply(To0d to0d, To2RedirectEntry blobEntry) throws IOException {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();

      //todo: get wait seconds
      long waitSeconds = to0d.getWaitSeconds();

      //todo: test owner public key and guid deniy list

      final OwnershipVoucherHeader header =
          Mapper.INSTANCE.readValue(to0d.getVoucher().getHeader(), OwnershipVoucherHeader.class);

      final byte[] data = Mapper.INSTANCE.writeValue(blobEntry);
      RvRedirect redirect = session.get(RvRedirect.class, header.getGuid().toString());
      if (redirect == null) {
        redirect = new RvRedirect();
        redirect.setGuid(header.getGuid().toString());
        redirect.setData(data);
        redirect.setExpiry(
            new Date(System.currentTimeMillis()
                + Duration.ofSeconds(waitSeconds).toMillis()));
        redirect.setCreatedOn(new Date(System.currentTimeMillis()));

        session.save(redirect);
      } else {
        redirect.setData(data);
        redirect.setExpiry(
            new Date(System.currentTimeMillis()
                + Duration.ofSeconds(waitSeconds).toMillis()));
        session.update(redirect);
      }
      trans.commit();

      return waitSeconds;

    } finally {
      session.close();
    }
  }
}
