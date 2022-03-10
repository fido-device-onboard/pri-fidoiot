// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.ResourceNotFoundException;
import org.fidoalliance.fdo.protocol.dispatch.VoucherQueryFunction;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardVoucherQueryFunction implements VoucherQueryFunction {

  @Override
  public OwnershipVoucher apply(String s) throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      OnboardingVoucher onboardingVoucher =
          session.find(OnboardingVoucher.class, s);

      trans.commit();
      if (onboardingVoucher == null) {
        throw new ResourceNotFoundException("guid: " + s);
      }
      return Mapper.INSTANCE.readValue(
          onboardingVoucher.getData(), OwnershipVoucher.class);


    } finally {
      session.close();
    }
  }
}
