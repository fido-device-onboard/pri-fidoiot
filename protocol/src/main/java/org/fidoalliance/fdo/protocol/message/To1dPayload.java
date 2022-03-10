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
@JsonPropertyOrder({"addressEntries", "to1ToTo0Hash"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To1dPayload {

  @JsonProperty("addressEntries")
  private To2AddressEntries addressEntries;

  @JsonProperty("to1ToTo0Hash")
  private Hash to1ToTo0Hash;

  @JsonIgnore
  public To2AddressEntries getAddressEntries() {
    return addressEntries;
  }

  @JsonIgnore
  public Hash getTo1ToTo0Hash() {
    return to1ToTo0Hash;
  }

  @JsonIgnore
  public void setAddressEntries(To2AddressEntries addressEntries) {
    this.addressEntries = addressEntries;
  }

  @JsonIgnore
  public void setTo1ToTo0Hash(Hash to1ToTo0Hash) {
    this.to1ToTo0Hash = to1ToTo0Hash;
  }
}
