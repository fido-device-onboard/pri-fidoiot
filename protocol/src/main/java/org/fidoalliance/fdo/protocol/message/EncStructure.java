// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"context", "protectedHeader", "external"})
@JsonSerialize(using = GenericArraySerializer.class)
public class EncStructure {

  @JsonProperty("context")
  private String context;

  @JsonProperty("protectedHeader")
  private byte[] protectedHeader;

  @JsonProperty("external")
  private byte[] external;

  public String getContext() {
    return context;
  }

  public byte[] getProtectedHeader() {
    return protectedHeader;
  }

  public byte[] getExternal() {
    return external;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public void setProtectedHeader(byte[] protectedHeader) {
    this.protectedHeader = protectedHeader;
  }

  public void setExternal(byte[] external) {
    this.external = external;
  }
}
