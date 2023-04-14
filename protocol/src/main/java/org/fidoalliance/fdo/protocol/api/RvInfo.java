// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.RvData;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;

/**
 * Maintains RV Info.
 */
public class RvInfo extends RestApi {
  protected static final LoggerService logger = new LoggerService(RvInfo.class);

  @Override
  protected void doGet() throws Exception {
    getTransaction();
    RvData rviData =
        getSession().get(RvData.class, Long.valueOf(1));


    String body = rviData.getData().getSubString(1,
        Long.valueOf(rviData.getData().length()).intValue());
    logger.info("RVInfo doGet body : " + body);

    getResponse().getWriter().print(body);
  }

  @Override
  public void doPost() throws Exception {

    String body = getStringBody();
    logger.info("RVInfo doPost body : " + body);
    getTransaction();

    Mapper.INSTANCE.readJsonValue(body, RendezvousInfo.class);

    RvData rviData =
        getSession().get(RvData.class, Long.valueOf(1));

    if (rviData == null) {
      rviData = new RvData();
      rviData.setData(getSession().getLobHelper().createClob(body));
      getSession().save(rviData);

    } else {
      rviData.setData(getSession().getLobHelper().createClob(body));
      getSession().update(rviData);
    }
  }
}
