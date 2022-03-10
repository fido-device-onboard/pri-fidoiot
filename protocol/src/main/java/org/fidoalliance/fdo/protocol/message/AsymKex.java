// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"randomSize", "b", "party"})
public class AsymKex {

  @JsonProperty("randomSize")
  private int randomSize;

  @JsonProperty("b")
  private byte[] beValue;

  @JsonProperty("party")
  KexParty party;

  public int getRandomSize() {
    return randomSize;
  }

  public byte[] getB() {
    return beValue;
  }

  public KexParty getParty() {
    return party;
  }

  public void setRandomSize(int randomSize) {
    this.randomSize = randomSize;
  }

  public void setB(byte[] beValue) {
    this.beValue = beValue;
  }

  public void setParty(KexParty party) {
    this.party = party;
  }
}
