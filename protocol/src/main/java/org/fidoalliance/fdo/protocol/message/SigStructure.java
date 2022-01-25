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
@JsonPropertyOrder({"context", "protectedBody", "externalData", "payload"})
@JsonSerialize(using = GenericArraySerializer.class)
public class SigStructure {

  @JsonProperty("context")
  private String context;

  @JsonProperty("protectedBody")
  private byte[] protectedBody;

  @JsonProperty("externalData")
  private byte[] externalData;

  @JsonProperty("payload")
  private byte[] payload;

  @JsonIgnore
  public String getContext() {
    return context;
  }

  @JsonIgnore
  public byte[] getProtectedBody() {
    return protectedBody;
  }

  @JsonIgnore
  public byte[] getExternalData() {
    return externalData;
  }

  @JsonIgnore
  public byte[] getPayload() {
    return payload;
  }

  @JsonIgnore
  public void setContext(String context) {
    this.context = context;
  }

  @JsonIgnore
  public void setProtectedBody(byte[] protectedBody) {
    this.protectedBody = protectedBody;
  }

  @JsonIgnore
  public void setExternalData(byte[] externalData) {
    this.externalData = externalData;
  }

  @JsonIgnore
  public void setPayload(byte[] payload) {
    this.payload = payload;
  }
}
