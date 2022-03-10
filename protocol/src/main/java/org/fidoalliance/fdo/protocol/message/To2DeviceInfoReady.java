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
@JsonPropertyOrder({"hmac", "maxMsgSize"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To2DeviceInfoReady {


  @JsonProperty("hmac")
  private Hash hmac;

  @JsonProperty("maxMsgSize")
  private Integer maxMessageSize;

  @JsonIgnore
  public Hash getHmac() {
    return hmac;
  }

  @JsonIgnore
  public Integer getMaxMessageSize() {
    return maxMessageSize;
  }

  @JsonIgnore
  public void setHmac(Hash hmac) {
    this.hmac = hmac;
  }

  @JsonIgnore
  public void setMaxMessageSize(Integer maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }
}
