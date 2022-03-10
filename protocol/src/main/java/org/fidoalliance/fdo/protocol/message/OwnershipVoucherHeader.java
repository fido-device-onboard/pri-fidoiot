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
@JsonPropertyOrder({"version", "guid", "rendezvousInfo", "deviceInfo", "publicKey",
    "certHash"})
@JsonSerialize(using = GenericArraySerializer.class)
public class OwnershipVoucherHeader {

  @JsonProperty("version")
  private ProtocolVersion version;

  @JsonProperty("guid")
  private Guid guid;

  @JsonProperty("rendezvousInfo")
  private RendezvousInfo rendezvousInfo;

  @JsonProperty("deviceInfo")
  private String deviceInfo;

  @JsonProperty("publicKey")
  private OwnerPublicKey publicKey;

  @JsonProperty("certHash")
  private Hash certHash;

  @JsonIgnore
  public ProtocolVersion getVersion() {
    return version;
  }

  @JsonIgnore
  public Guid getGuid() {
    return guid;
  }

  @JsonIgnore
  public RendezvousInfo getRendezvousInfo() {
    return rendezvousInfo;
  }

  @JsonIgnore
  public String getDeviceInfo() {
    return deviceInfo;
  }

  @JsonIgnore
  public OwnerPublicKey getPublicKey() {
    return publicKey;
  }

  @JsonIgnore
  public Hash getCertHash() {
    return certHash;
  }

  @JsonIgnore
  public void setVersion(ProtocolVersion version) {
    this.version = version;
  }

  @JsonIgnore
  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  @JsonIgnore
  public void setRendezvousInfo(RendezvousInfo rendezvousInfo) {
    this.rendezvousInfo = rendezvousInfo;
  }

  @JsonIgnore
  public void setDeviceInfo(String deviceInfo) {
    this.deviceInfo = deviceInfo;
  }

  @JsonIgnore
  public void setPublicKey(OwnerPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  @JsonIgnore
  public void setCertHash(Hash certHash) {
    this.certHash = certHash;
  }
}


