// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.time.Duration;
import org.apache.commons.lang3.function.FailableSupplier;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.hibernate.Session;
import org.hibernate.Transaction;


public class OnboardConfigSupplier
    implements FailableSupplier<OnboardingConfig, IOException> {

  @Override
  public OnboardingConfig get() throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      OnboardingConfig onboardConfig =
          session.find(OnboardingConfig.class,Long.valueOf(1));

      if (onboardConfig == null) {
        onboardConfig = new OnboardingConfig();
        onboardConfig.setMaxMessageSize(null);
        onboardConfig.setMaxServiceInfoSize(null);
        onboardConfig.setWaitSeconds(Duration.ofDays(1).toSeconds());

        onboardConfig.setReplacementRvInfo(null);

        //3=http or 5=https
        final String defaultBob = ""
            + "- - \"127.0.0.1\"\n"
            + "  - \"localhost\"\n"
            + "  - %s\n"
            + "  - 3\n";

        final String defaultPort = Config.getWorker(HttpServer.class).getHttpPort();
        final String rviString = String.format(defaultBob, defaultPort);
        final To2AddressEntries entries =
            Mapper.INSTANCE.readValue(rviString, To2AddressEntries.class);
        onboardConfig.setRvBlob(Mapper.INSTANCE.writeValue(entries));


        session.persist(onboardConfig);


      }
      trans.commit();

      return onboardConfig;

    } finally {
      session.close();
    }
  }
}
