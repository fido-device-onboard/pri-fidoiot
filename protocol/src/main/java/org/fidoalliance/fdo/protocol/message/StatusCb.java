// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;


@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"completed", "retCode", "timeout"})
@JsonSerialize(using = GenericArraySerializer.class)
public class StatusCb {

  @JsonProperty("completed")
  boolean completed;

  @JsonProperty("retCode")
  int retCode;

  @JsonProperty("timeout")
  int timeout;

  public boolean isCompleted() {
    return completed;
  }

  public int getRetCode() {
    return retCode;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public void setRetCode(int retCode) {
    this.retCode = retCode;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}
