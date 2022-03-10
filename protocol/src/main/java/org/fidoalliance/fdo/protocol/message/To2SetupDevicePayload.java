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
@JsonPropertyOrder({"rvInfo", "guid", "nonce", "owner2Key"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To2SetupDevicePayload {

  @JsonProperty("rvInfo")
  private RendezvousInfo rendezvousInfo;

  @JsonProperty("guid")
  private Guid guid;

  @JsonProperty("nonce")
  private Nonce nonce;


  @JsonProperty("owner2Key")
  private OwnerPublicKey owner2Key;

  @JsonIgnore
  public RendezvousInfo getRendezvousInfo() {
    return rendezvousInfo;
  }

  @JsonIgnore
  public Guid getGuid() {
    return guid;
  }

  @JsonIgnore
  public Nonce getNonce() {
    return nonce;
  }

  @JsonIgnore
  public OwnerPublicKey getOwner2Key() {
    return owner2Key;
  }

  @JsonIgnore
  public void setRendezvousInfo(RendezvousInfo rendezvousInfo) {
    this.rendezvousInfo = rendezvousInfo;
  }

  @JsonIgnore
  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  @JsonIgnore
  public void setNonce(Nonce nonce) {
    this.nonce = nonce;
  }

  @JsonIgnore
  public void setOwner2Key(OwnerPublicKey owner2Key) {
    this.owner2Key = owner2Key;
  }
}
