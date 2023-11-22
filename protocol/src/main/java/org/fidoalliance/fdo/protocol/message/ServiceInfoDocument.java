// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;



public class ServiceInfoDocument {


  @JsonProperty("instructions")
  private String instructions;

  @JsonProperty("index")
  private int index;


  @JsonIgnore
  public String getInstructions() {
    return instructions;
  }


  @JsonIgnore
  public int getIndex() {
    return index;
  }

  @JsonIgnore
  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }

  @JsonIgnore
  public void setIndex(int index) {
    this.index = index;
  }

}
