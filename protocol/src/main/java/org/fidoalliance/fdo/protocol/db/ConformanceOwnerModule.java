// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.time.Duration;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.entity.ConformanceData;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.DevModList;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.ServiceInfo;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.fidoalliance.fdo.protocol.serviceinfo.DevMod;
import org.fidoalliance.fdo.protocol.serviceinfo.FidoAlliance;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ConformanceOwnerModule implements ServiceInfoModule {

  @Override
  public String getName() {
    return FidoAlliance.NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {
    state.setExtra(AnyType.fromObject(new ServiceInfoQueue()));
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {
    switch (kvPair.getKey()) {
      case DevMod.KEY_MODULES: {
        DevModList list =
            Mapper.INSTANCE.readValue(kvPair.getValue(), DevModList.class);
        for (String name : list.getModulesNames()) {
          if (name.equals(FidoAlliance.NAME)) {
            state.setActive(true);
            ServiceInfoQueue queue = state.getExtra().covertValue(ServiceInfoQueue.class);
            ServiceInfoKeyValuePair activePair = new ServiceInfoKeyValuePair();
            activePair.setKeyName(FidoAlliance.ACTIVE);
            activePair.setValue(Mapper.INSTANCE.writeValue(true));
            queue.add(activePair);
            getConformance(state.getGuid(), queue);
            state.setExtra(AnyType.fromObject(queue));
          }
        }
      }
      break;
      default:
        break;
    }

  }

  @Override
  public void keepAlive() throws IOException {

  }

  @Override
  public void send(ServiceInfoModuleState state, ServiceInfoSendFunction sendFunction)
      throws IOException {


    ServiceInfoQueue queue = state.getExtra().covertValue(ServiceInfoQueue.class);
    while (queue.size() > 0) {
      boolean sent = sendFunction.apply(queue.peek());
      if (sent) {
        queue.poll();
      } else {
        break;
      }
    }

    if (queue.size() == 0) {
      state.setDone(true);
    }
    state.setExtra(AnyType.fromObject(queue));
  }


  private void getConformance(Guid guid, ServiceInfoQueue queue) throws IOException {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();

      ConformanceData confData =
          session.find(ConformanceData.class, guid.toString());

      if (confData != null) {

        ServiceInfoKeyValuePair keyValuePair = new ServiceInfoKeyValuePair();
        keyValuePair.setKeyName(FidoAlliance.DEV_CONFORMANCE);
        keyValuePair.setValue(Mapper.INSTANCE.writeValue(confData.getData()));
        queue.add(keyValuePair);
      }
      trans.commit();


    } finally {
      session.close();
    }
  }
}
