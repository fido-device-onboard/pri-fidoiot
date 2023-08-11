// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.db.FdoSimCsrOwnerModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoModule;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;

public class FdoSimCsrDeviceModule implements ServiceInfoModule {

  private static final LoggerService logger = new LoggerService(FdoSimCsrDeviceModule.class);
  private final ServiceInfoQueue queue = new ServiceInfoQueue();


  @Override
  public String getName() {
    return FdoSimCsrOwnerModule.MODULE_NAME;
  }

  @Override
  public void prepare(ServiceInfoModuleState state) throws IOException {

    //add to queue csr requests
  }

  @Override
  public void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair)
      throws IOException {

    switch (kvPair.getKey()) {
      case FdoSimCsrOwnerModule.ACTIVE:
        logger.info(FdoSimCsrOwnerModule.ACTIVE + " = "
            + Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        state.setActive(Mapper.INSTANCE.readValue(kvPair.getValue(), Boolean.class));
        break;
      case FdoSimCsrOwnerModule.CACERTS_RES:
        if (state.isActive()) {
          logger.info("CA response");
          logger.info(Mapper.INSTANCE.readValue(kvPair.getValue(), String.class));

        }
        break;
      case FdoSimCsrOwnerModule.SIMPLEENROLL_RES:
        if (state.isActive()) {
          logger.info("Simple response");
          logger.info(Mapper.INSTANCE.readValue(kvPair.getValue(), String.class));
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
    while (queue.size() > 0) {
      boolean sent = sendFunction.apply(queue.peek());
      if (sent) {
        queue.poll();
      } else {
        break;
      }
    }

  }

}
