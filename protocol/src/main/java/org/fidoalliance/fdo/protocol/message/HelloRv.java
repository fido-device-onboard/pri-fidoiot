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
@JsonPropertyOrder({"guid", "sigInfoA"})
@JsonSerialize(using = GenericArraySerializer.class)
public class HelloRv {

  @JsonProperty("guid")
  private Guid guid;

  @JsonProperty("sigInfoA")
  private SigInfo sigInfo;

  @JsonIgnore
  public Guid getGuid() {
    return guid;
  }

  @JsonIgnore
  public SigInfo getSigInfo() {
    return sigInfo;
  }

  @JsonIgnore
  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  @JsonIgnore
  public void setSigInfo(SigInfo sigInfo) {
    this.sigInfo = sigInfo;
  }
}