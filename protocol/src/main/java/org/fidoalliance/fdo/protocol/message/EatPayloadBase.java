// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.EatPayloadBaseDeserializer;
import org.fidoalliance.fdo.protocol.serialization.EatPayloadBaseSerializer;

@JsonSerialize(using = EatPayloadBaseSerializer.class)
@JsonDeserialize(using = EatPayloadBaseDeserializer.class)
public class EatPayloadBase {

  private Nonce nonce;
  private Guid guid;
  private AnyType fdoClaim;

  public Nonce getNonce() {
    return nonce;
  }

  public Guid getGuid() {
    return guid;
  }

  public AnyType getFdoClaim() {
    return fdoClaim;
  }

  public void setNonce(Nonce nonce) {
    this.nonce = nonce;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public void setFdoClaim(AnyType fdoClaim) {
    this.fdoClaim = fdoClaim;
  }
}
