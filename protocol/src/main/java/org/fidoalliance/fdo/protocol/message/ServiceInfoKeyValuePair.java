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
@JsonPropertyOrder({"key", "value"})
@JsonSerialize(using = GenericArraySerializer.class)
public class ServiceInfoKeyValuePair {

  @JsonProperty("key")
  private String key;

  @JsonProperty("value")
  private byte[] value;

  @JsonIgnore
  public String getKey() {
    return key;
  }

  @JsonIgnore
  public byte[] getValue() {
    return value;
  }

  @JsonIgnore
  public void setKeyName(String key) {
    this.key = key;
  }

  @JsonIgnore
  public void setValue(byte[] value) {
    this.value = value;
  }


}
