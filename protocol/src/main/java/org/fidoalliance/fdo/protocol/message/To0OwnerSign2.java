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
@JsonPropertyOrder({"to0d", "to1d"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To0OwnerSign2 {

  @JsonProperty("to0d")
  private To0d to0d;

  @JsonProperty("to1d")
  private CoseSign1 to1d;

  @JsonIgnore
  public To0d getTo0d() {
    return to0d;
  }

  @JsonIgnore
  public CoseSign1 getTo1d() {
    return to1d;
  }

  @JsonIgnore
  public void setTo0d(To0d to0d) {
    this.to0d = to0d;
  }

  @JsonIgnore
  public void setTo1d(CoseSign1 to1d) {
    this.to1d = to1d;
  }
}

