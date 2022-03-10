// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serviceinfo;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoSendFunction;
import org.fidoalliance.fdo.protocol.message.ServiceInfo;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;

public class StandardServiceInfoSendFunction implements ServiceInfoSendFunction {

  private final int mtu;
  private final ServiceInfo serviceInfo;

  public StandardServiceInfoSendFunction(int mtu, ServiceInfo serviceInfo) {
    this.mtu = mtu;
    this.serviceInfo = serviceInfo;
  }

  @Override
  public Boolean apply(ServiceInfoKeyValuePair keyValuePair) throws IOException {
    serviceInfo.add(keyValuePair);
    byte[] data = Mapper.INSTANCE.writeValue(serviceInfo);
    if (data.length > mtu) {
      serviceInfo.removeLast();
      return false;
    }
    return true;
  }
}
