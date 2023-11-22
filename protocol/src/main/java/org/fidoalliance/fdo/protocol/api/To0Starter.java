// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.StandardTo0Client;
import org.fidoalliance.fdo.protocol.db.OnboardConfigSupplier;
import org.fidoalliance.fdo.protocol.db.To2BlobSupplier;
import org.fidoalliance.fdo.protocol.dispatch.VoucherQueryFunction;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.entity.VoucherAlias;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.To0OwnerSign;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;

/**
 * API to start Starts To0 protocol.
 */
public class To0Starter extends RestApi {

  private boolean isGuid(String value) {
    try {
      UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return true;
  }

  @Override
  public void doGet() throws Exception {

    LoggerService logger = new LoggerService(To0Starter.class);
    String path = getLastSegment();

    //if last segment is serialno vs guid
    if (!isGuid(path)) {
      VoucherAlias voucherAlias = getSession().get(VoucherAlias.class, path);
      if (voucherAlias != null) {
        path = voucherAlias.getGuid();
      }
    }

    final String guid = path;

    Thread thread = new Thread() {
      public void run() {
        try {
          logger.info("Triggering TO0 for GUID: " + guid);
          OwnershipVoucher voucher = Config.getWorker(VoucherQueryFunction.class).apply(
              guid);
          StandardTo0Client to0Client = new StandardTo0Client();
          To0d to0d = new To0d();
          to0d.setVoucher(voucher);
          to0d.setWaitSeconds(Duration.ofDays(1).toSeconds());

          OnboardingConfig onboardConfig = new OnboardConfigSupplier().get();

          To2AddressEntries addressEntries = new To2BlobSupplier().get();

          to0Client.setAddressEntries(addressEntries);

          to0d.setWaitSeconds(onboardConfig.getWaitSeconds());
          to0Client.setTo0d(to0d);
          to0Client.run();
        } catch (IOException e) {
          logger.error("TO0 failed for GUID: " + guid);
        }

      }
    };

    thread.start();

  }
}
