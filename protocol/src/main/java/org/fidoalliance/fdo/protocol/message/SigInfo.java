// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0
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
@JsonPropertyOrder({"sigInfoType", "info",})
@JsonSerialize(using = GenericArraySerializer.class)
public class SigInfo {

  @JsonProperty("sigInfoType")
  private SigInfoType sigInfoType;

  @JsonProperty("info")
  private byte[] info = new byte[0];

  @JsonIgnore
  public SigInfoType getSigInfoType() {
    return sigInfoType;
  }

  @JsonIgnore
  public byte[] getInfo() {
    return info;
  }

  @JsonIgnore
  public void setSigInfoType(SigInfoType sigInfoType) {
    this.sigInfoType = sigInfoType;
  }

  @JsonIgnore
  public void setInfo(byte[] info) {
    this.info = info;
  }
}
