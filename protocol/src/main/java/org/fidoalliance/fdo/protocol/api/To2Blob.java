package org.fidoalliance.fdo.protocol.api;

import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.entity.RvData;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;

public class To2Blob extends RestApi {

  @Override
  public void doPost() throws Exception {
    String body = getStringBody();
    getTransaction();

    To2AddressEntries entries =
        Mapper.INSTANCE.readJsonValue(body, To2AddressEntries.class);



    OnboardingConfig onboardingConfig =
        getSession().get(OnboardingConfig.class,Long.valueOf(1));

    if (onboardingConfig == null) {
      onboardingConfig = new OnboardingConfig();
      onboardingConfig.setRvBlob(Mapper.INSTANCE.writeValue(entries));
      getSession().save(onboardingConfig);

    } else {
      onboardingConfig.setRvBlob(Mapper.INSTANCE.writeValue(entries));
      getSession().update(onboardingConfig);
    }
  }
}
