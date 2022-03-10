// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.RvData;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;

/**
 * Maintains RV Info.
 */
public class RvInfo extends RestApi {

  @Override
  public void doPost() throws Exception {


    String body = getStringBody();
    getTransaction();


    RendezvousInfo info = Mapper.INSTANCE.readJsonValue(body,RendezvousInfo.class);


    RvData rviData =
        getSession().get(RvData.class,Long.valueOf(1));

    if (rviData == null) {
      rviData = new RvData();
      rviData.setData(Mapper.INSTANCE.writeValue(info));
      getSession().save(rviData);

    } else {
      rviData.setData(Mapper.INSTANCE.writeValue(info));
      getSession().update(rviData);
    }
  }
}
