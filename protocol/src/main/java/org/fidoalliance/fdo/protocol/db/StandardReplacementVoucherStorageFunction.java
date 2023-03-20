// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.util.Date;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ReplacementVoucherStorageFunction;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucherHeader;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardReplacementVoucherStorageFunction implements
    ReplacementVoucherStorageFunction {
  private static final LoggerService
          logger = new LoggerService(StandardReplacementVoucherStorageFunction.class);


  @Override
  public String apply(OwnershipVoucher voucher1, OwnershipVoucher voucher2) throws IOException {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      Transaction trans = session.beginTransaction();
      OwnershipVoucherHeader header1 = Mapper.INSTANCE.readValue(voucher1.getHeader(),
          OwnershipVoucherHeader.class);

      OnboardingVoucher onboardingVoucher = session.find(OnboardingVoucher.class,
          header1.getGuid().toString());
      if (onboardingVoucher != null) {
        if (voucher2.getHmac() != null) {
          onboardingVoucher.setReplacement(Mapper.INSTANCE.writeValue(voucher2));
        } else {
          onboardingVoucher.setReplacement(null);
        }
        onboardingVoucher.setTo2CompletedOn(new Date(System.currentTimeMillis()));
        session.update(onboardingVoucher);
        trans.commit();
      }
      if (voucher2.getHmac() != null) {
        OwnershipVoucherHeader header2 = Mapper.INSTANCE.readValue(voucher2.getHeader(),
            OwnershipVoucherHeader.class);
        return header2.getGuid().toString();
      } else {
        return header1.getGuid().toString();
      }


    } finally {
      logger.debug("Closing Replacement Voucher Storage Function's session");
      session.close();
    }
  }
}
