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
@JsonPropertyOrder({"previousHash", "headerHash", "extra", "ownerPubKey"})
@JsonSerialize(using = GenericArraySerializer.class)
public class OwnershipVoucherEntryPayload {

  @JsonProperty("previousHash")
  private Hash previousHash;

  @JsonProperty("headerHash")
  private Hash headerHash;

  @JsonProperty("extra")
  private byte[] extra;

  @JsonProperty("ownerPubKey")
  OwnerPublicKey ownerPublicKey;

  @JsonIgnore
  public Hash getPreviousHash() {
    return previousHash;
  }

  @JsonIgnore
  public Hash getHeaderHash() {
    return headerHash;
  }

  @JsonIgnore
  public byte[] getExtra() {
    return extra;
  }

  @JsonIgnore
  public OwnerPublicKey getOwnerPublicKey() {
    return ownerPublicKey;
  }

  @JsonIgnore
  public void setPreviousHash(Hash previousHash) {
    this.previousHash = previousHash;
  }

  @JsonIgnore
  public void setHeaderHash(Hash headerHash) {
    this.headerHash = headerHash;
  }

  @JsonIgnore
  public void setExtra(byte[] extra) {
    this.extra = extra;
  }

  @JsonIgnore
  public void setOwnerPublicKey(OwnerPublicKey ownerPublicKey) {
    this.ownerPublicKey = ownerPublicKey;
  }
}
