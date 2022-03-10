// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"more", "done", "svi"})
@JsonSerialize(using = GenericArraySerializer.class)
public class OwnerServiceInfo {

  @JsonProperty("more")
  private boolean more;

  @JsonProperty("done")
  private boolean done;

  @JsonProperty("svi")
  private ServiceInfo serviceInfo;

  @JsonIgnore
  public boolean isMore() {
    return more;
  }

  @JsonIgnore
  public boolean isDone() {
    return done;
  }

  @JsonIgnore
  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  @JsonIgnore
  public void setMore(boolean more) {
    this.more = more;
  }

  @JsonIgnore
  public void setDone(boolean done) {
    this.done = done;
  }

  @JsonIgnore
  public void setServiceInfo(ServiceInfo serviceInfo) {
    this.serviceInfo = serviceInfo;
  }
}
