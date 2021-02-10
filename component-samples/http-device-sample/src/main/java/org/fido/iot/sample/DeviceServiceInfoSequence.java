// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import org.fido.iot.serviceinfo.ServiceInfoSequence;

public class DeviceServiceInfoSequence extends ServiceInfoSequence {

  private Object serviceInfoValue;
  private long serviceInfoLength;

  /**
   * Constructor.
   *
   * @param id service info identifier that is used to track service info
   * @param length length of content. In case length param is not being used
   *               it can be passed as 0.
   */
  public DeviceServiceInfoSequence(String id, Object value, long length) {
    super(id);
    serviceInfoValue = value;
    serviceInfoLength = length;
  }

  @Override
  public long length() {
    return serviceInfoLength;
  }

  @Override
  public Object getContent() {
    return serviceInfoValue;
  }

}
