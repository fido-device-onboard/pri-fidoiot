// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import org.apache.commons.lang3.function.FailableSupplier;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class To2BlobSupplier implements FailableSupplier<To2AddressEntries, IOException> {

  @Override
  public To2AddressEntries get() throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      OnboardingConfig onboardConfig =
          session.find(OnboardingConfig.class, Long.valueOf(1));

      if (onboardConfig == null) {
        onboardConfig = new OnboardingConfig();
        onboardConfig.setMaxMessageSize(null);
        onboardConfig.setMaxServiceInfoSize(null);
        onboardConfig.setWaitSeconds(Duration.ofDays(1).toSeconds());

        onboardConfig.setReplacementRvInfo(null);

        //3=http or 5=https
        final String defaultBob = "[[null,\"host.docker.internal\",%s,3]]";

        final String defaultPort = Config.getWorker(HttpServer.class).getHttpPort();
        final String rviString = String.format(defaultBob, defaultPort);

        onboardConfig.setRvBlob(session.getLobHelper().createClob(rviString));

        session.persist(onboardConfig);


      }
      String body = onboardConfig.getRvBlob().getSubString(1,
          Long.valueOf(onboardConfig.getRvBlob().length()).intValue());
      trans.commit();

      return Mapper.INSTANCE.readJsonValue(body,To2AddressEntries.class);


    } catch (SQLException throwables) {
      throw new InternalServerErrorException(throwables);
    } finally {
      session.close();
    }
  }
}
