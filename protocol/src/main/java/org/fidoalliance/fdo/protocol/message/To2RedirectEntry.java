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
@JsonPropertyOrder({"to1d", "chain"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To2RedirectEntry {

  @JsonProperty("to1d")
  private CoseSign1 to1d;
  @JsonProperty("chain")
  private CertChain certChain;

  @JsonIgnore
  public CoseSign1 getTo1d() {
    return to1d;
  }

  @JsonIgnore
  public CertChain getCertChain() {
    return certChain;
  }

  @JsonIgnore
  public void setTo1d(CoseSign1 to1d) {
    this.to1d = to1d;
  }

  @JsonIgnore
  public void setCertChain(CertChain certChain) {
    this.certChain = certChain;
  }
}
