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
    state.setExtra(AnyType.fromObject(new ServiceInfo()));
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {
    switch (kvPair.getKey()) {
      case DevMod.KEY_MODULES: {
        DevModList list = kvPair.getValue().unwrap(DevModList.class);
        for (int i = 2; i < list.size(); i++) {
          if (list.get(i).covertValue(String.class).equals(FidoAlliance.NAME)) {
            state.setActive(true);
            ServiceInfo info = state.getExtra().covertValue(ServiceInfo.class);
            ServiceInfoKeyValuePair activePair = new ServiceInfoKeyValuePair();
            activePair.setKeyName(FidoAlliance.ACTIVE);
            AnyType value = AnyType.fromObject(true);
            value.wrap();
            activePair.setValue(value);
            info.add(activePair);
            getConformance(state.getGuid(),info);
            state.setExtra(AnyType.fromObject(info));
          }
        }
      }
      break;
      default:
        break;
    }

  }

  @Override
  public void send(ServiceInfoModuleState state, ServiceInfoSendFunction sendFunction)
      throws IOException {

    ServiceInfo serviceInfo = state.getExtra().covertValue(ServiceInfo.class);
    while (serviceInfo.size() > 0) {
      boolean sent = sendFunction.apply(serviceInfo.getFirst());
      if (sent) {
        serviceInfo.removeFirst();
      } else {
        break;
      }
    }
    if (serviceInfo.size() == 0) {
      state.setDone(true);
    }
    state.setExtra(AnyType.fromObject(serviceInfo));
  }

  private void getConformance(Guid guid,ServiceInfo serviceInfo) throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();

      ConformanceData confData =
          session.find(ConformanceData.class,guid.toString());

      if (confData != null) {

        ServiceInfoKeyValuePair keyValuePair = new ServiceInfoKeyValuePair();
        keyValuePair.setKeyName(FidoAlliance.DEV_CONFORMANCE);
        AnyType value = AnyType.fromObject(confData.getData());
        value.wrap();
        keyValuePair.setValue(value);
        serviceInfo.add(keyValuePair);
      }
      trans.commit();


    } finally {
      session.close();
    }
  }
}
