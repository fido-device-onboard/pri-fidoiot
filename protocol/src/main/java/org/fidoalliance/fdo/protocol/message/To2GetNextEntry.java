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
@JsonPropertyOrder({"entryNum"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To2GetNextEntry {

  @JsonProperty("entryNum")
  private int entryNum;

  @JsonIgnore
  public int getEntryNum() {
    return entryNum;
  }

  @JsonIgnore
  public void setEntryNum(int entryNum) {
    this.entryNum = entryNum;
  }
}
