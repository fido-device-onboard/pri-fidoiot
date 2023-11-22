// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.serviceinfo.FidoAlliance;

public class ConformanceDeviceModule implements ServiceInfoModule {

  private final LoggerService logger = new LoggerService(ConformanceDeviceModule.class);


  @Override
  public String getName() {
    return FidoAlliance.NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {

  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {

    switch (kvPair.getKey()) {
      case FidoAlliance.ACTIVE:
        logger.info(FidoAlliance.ACTIVE + " = "
            + Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        state.setActive(true);
        break;
      case FidoAlliance.DEV_CONFORMANCE:
        if (state.isActive()) {
          logger.info(FidoAlliance.DEV_CONFORMANCE + " = "
              + Mapper.INSTANCE.readValue(kvPair.getValue(), String.class));
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

  }
}