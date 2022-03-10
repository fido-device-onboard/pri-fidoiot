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
@JsonPropertyOrder({"version", "header", "hmac", "certChain", "entries"})
@JsonSerialize(using = GenericArraySerializer.class)
public class OwnershipVoucher {

  @JsonProperty("version")
  ProtocolVersion version = ProtocolVersion.current();

  @JsonProperty("header")
  byte[] header;

  @JsonProperty("hmac")
  Hash hmac;

  @JsonProperty("certChain")
  CertChain certChain;

  @JsonProperty("entries")
  OwnershipVoucherEntries entries;

  @JsonIgnore
  public ProtocolVersion getVersion() {
    return version;
  }

  @JsonIgnore
  public byte[] getHeader() {
    return header;
  }

  @JsonIgnore
  public Hash getHmac() {
    return hmac;
  }

  @JsonIgnore
  public CertChain getCertChain() {
    return certChain;
  }

  @JsonIgnore
  public OwnershipVoucherEntries getEntries() {
    return entries;
  }

  @JsonIgnore
  public void setVersion(ProtocolVersion version) {
    this.version = version;
  }

  @JsonIgnore
  public void setHeader(byte[] header) {
    this.header = header;
  }

  @JsonIgnore
  public void setHmac(Hash hmac) {
    this.hmac = hmac;
  }

  @JsonIgnore
  public void setCertChain(CertChain certChain) {
    this.certChain = certChain;
  }

  @JsonIgnore
  public void setEntries(OwnershipVoucherEntries entries) {
    this.entries = entries;
  }
}
