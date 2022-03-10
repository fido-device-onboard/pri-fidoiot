// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.FdoSysInstruction;
import org.fidoalliance.fdo.protocol.entity.SystemPackage;
import org.hibernate.engine.jdbc.ClobProxy;

/**
 * Maintains Service Info Packages.
 */
public class SviPackage extends RestApi {

  LoggerService logger =  new LoggerService(SviPackage.class);

  @Override
  public void doGet() throws Exception {
    getTransaction();

    SystemPackage systemPackage =
        getSession().find(SystemPackage.class,Long.valueOf(1));

    if (systemPackage != null) {
      String result = systemPackage.getData().getSubString(1,
          Long.valueOf(systemPackage.getData().length()).intValue());
      getResponse().getWriter().print(result);
    }
  }

  @Override
  public void doPost() throws Exception {
    String body = getStringBody();
    getTransaction();
    try {
      FdoSysInstruction[] instructions =
          Mapper.INSTANCE.readJsonValue(body, FdoSysInstruction[].class);
      if (instructions.length <= 0) {
        logger.warn("Empty SVI instruction.");
      }
    } catch (Exception e) {
      logger.error("Received invalid SVI instruction.");
    }

    SystemPackage systemPackage =
        getSession().find(SystemPackage.class,Long.valueOf(1));

    if (systemPackage == null) {
      systemPackage = new SystemPackage();
    }
    systemPackage.setData(ClobProxy.generateProxy(body));
    getSession().persist(systemPackage);
  }
}
