// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"keyType", "encoded", "party"})
public class EcdhKex {

  @JsonProperty("keyType")
  PublicKeyType keyType;

  @JsonProperty("encoded")
  byte[] encodedKey;

  @JsonProperty("party")
  KexParty party;

  public PublicKeyType getKeyType() {
    return keyType;
  }

  public byte[] getEncodedKey() {
    return encodedKey;
  }

  public KexParty getParty() {
    return party;
  }

  public void setKeyType(PublicKeyType keyType) {
    this.keyType = keyType;
  }

  public void setEncodedKey(byte[] encodedKey) {
    this.encodedKey = encodedKey;
  }

  public void setParty(KexParty party) {
    this.party = party;
  }
}
