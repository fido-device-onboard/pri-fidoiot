// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"message", "state"})
public class KexMessage {

  @JsonProperty("message")
  private byte[] message;

  @JsonProperty("state")
  private AnyType state;

  public byte[] getMessage() {
    return message;
  }

  public AnyType getState() {
    return state;
  }

  public void setMessage(byte[] message) {
    this.message = message;
  }

  public void setState(AnyType state) {
    this.state = state;
  }
}
