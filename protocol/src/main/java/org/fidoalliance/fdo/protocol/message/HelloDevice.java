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
@JsonPropertyOrder({"maxMessageSize", "guid", "proveTo2Ov", "kexSuiteName", "cipherSuiteType",
    "sigInfoA"})
@JsonSerialize(using = GenericArraySerializer.class)
public class HelloDevice {

  @JsonProperty("maxMessageSize")
  int maxMessageSize;

  @JsonProperty("guid")
  private Guid guid;

  @JsonProperty("proveTo2Ov")
  private Nonce proveTo2Ov;

  @JsonProperty("kexSuiteName")
  private String kexSuiteName;

  @JsonProperty("cipherSuiteType")
  private CipherSuiteType cipherSuiteType;

  @JsonProperty("sigInfoA")
  private SigInfo sigInfo;

  @JsonIgnore
  public int getMaxMessageSize() {
    return maxMessageSize;
  }

  @JsonIgnore
  public Guid getGuid() {
    return guid;
  }

  @JsonIgnore
  public Nonce getProveTo2Ov() {
    return proveTo2Ov;
  }

  @JsonIgnore
  public String getKexSuiteName() {
    return kexSuiteName;
  }

  @JsonIgnore
  public CipherSuiteType getCipherSuiteType() {
    return cipherSuiteType;
  }

  @JsonIgnore
  public SigInfo getSigInfo() {
    return sigInfo;
  }

  @JsonIgnore
  public void setMaxMessageSize(int maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }

  @JsonIgnore
  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  @JsonIgnore
  public void setProveTo2Ov(Nonce proveTo2Ov) {
    this.proveTo2Ov = proveTo2Ov;
  }

  @JsonIgnore
  public void setKexSuiteName(String kexSuiteName) {
    this.kexSuiteName = kexSuiteName;
  }

  @JsonIgnore
  public void setCipherSuiteName(CipherSuiteType cipherSuiteType) {
    this.cipherSuiteType = cipherSuiteType;
  }

  @JsonIgnore
  public void setSigInfo(SigInfo sigInfo) {
    this.sigInfo = sigInfo;
  }
}
