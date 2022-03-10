// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"active", "protVer", "hmacSecret", "deviceInfo", "guid", "rvInfo",
    "pubKeyHash"})
@JsonSerialize(using = GenericArraySerializer.class)
public class DeviceCredential {

  @JsonProperty("active")
  private boolean active;

  @JsonProperty("protVer")
  private ProtocolVersion protVer;

  @JsonProperty("hmacSecret")
  private byte[] hmacSecret; //confidentiality required

  @JsonProperty("deviceInfo")
  private String deviceInfo;

  @JsonProperty("guid")
  private Guid guid;

  @JsonProperty("rvInfo")
  private RendezvousInfo rvInfo;

  @JsonProperty("pubKeyHash")
  private Hash pubKeyHash;

  public boolean getActive() {
    return active;
  }

  public ProtocolVersion getProtVer() {
    return protVer;
  }

  public byte[] getHmacSecret() {
    return hmacSecret;
  }

  public String getDeviceInfo() {
    return deviceInfo;
  }

  public Guid getGuid() {
    return guid;
  }

  public RendezvousInfo getRvInfo() {
    return rvInfo;
  }

  public Hash getPubKeyHash() {
    return pubKeyHash;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public void setProtVer(ProtocolVersion protVer) {
    this.protVer = protVer;
  }

  public void setHmacSecret(byte[] hmacSecret) {
    this.hmacSecret = hmacSecret;
  }

  public void setDeviceInfo(String deviceInfo) {
    this.deviceInfo = deviceInfo;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public void setRvInfo(RendezvousInfo rvInfo) {
    this.rvInfo = rvInfo;
  }

  public void setPubKeyHash(Hash pubKeyHash) {
    this.pubKeyHash = pubKeyHash;
  }
}
