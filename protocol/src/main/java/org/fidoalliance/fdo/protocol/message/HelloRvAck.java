// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0
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
@JsonPropertyOrder({"nonceTo1Proof", "sigInfoB"})
@JsonSerialize(using = GenericArraySerializer.class)
public class HelloRvAck {

  @JsonProperty("nonceTo1Proof")
  private Nonce nonceTo1Proof;

  @JsonProperty("sigInfoB")
  private SigInfo sigInfoB;

  @JsonIgnore
  public Nonce getNonceTo1Proof() {
    return nonceTo1Proof;
  }

  @JsonIgnore
  public SigInfo getSigInfoB() {
    return sigInfoB;
  }

  @JsonIgnore
  public void setNonceTo1Proof(Nonce nonceTo1Proof) {
    this.nonceTo1Proof = nonceTo1Proof;
  }

  @JsonIgnore
  public void setSigInfoB(SigInfo sigInfoB) {
    this.sigInfoB = sigInfoB;
  }
}
