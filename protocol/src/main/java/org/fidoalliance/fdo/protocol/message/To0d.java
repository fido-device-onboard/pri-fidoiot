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
@JsonPropertyOrder({"voucher", "waitSeconds", "nonce"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To0d {

  @JsonProperty("voucher")
  private OwnershipVoucher voucher;

  @JsonProperty("waitSeconds")
  private long waitSeconds;

  @JsonProperty("nonce")
  private Nonce nonce;

  @JsonIgnore
  public OwnershipVoucher getVoucher() {
    return voucher;
  }

  @JsonIgnore
  public long getWaitSeconds() {
    return waitSeconds;
  }

  @JsonIgnore
  public Nonce getNonce() {
    return nonce;
  }

  @JsonIgnore
  public void setVoucher(OwnershipVoucher voucher) {
    this.voucher = voucher;
  }

  @JsonIgnore
  public void setWaitSeconds(long waitSeconds) {
    this.waitSeconds = waitSeconds;
  }

  @JsonIgnore
  public void setNonce(Nonce nonce) {
    this.nonce = nonce;
  }
}
