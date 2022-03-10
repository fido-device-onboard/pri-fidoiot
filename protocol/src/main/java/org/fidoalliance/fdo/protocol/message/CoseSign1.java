// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;
import org.fidoalliance.fdo.protocol.serialization.TaggedItem;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"protectedHeader", "unprotectedHeader", "payload", "signature"})
@JsonSerialize(using = GenericArraySerializer.class)
public class CoseSign1 implements TaggedItem {

  @JsonProperty("protectedHeader")
  byte[] protectedHeader;

  @JsonProperty("unprotectedHeader")
  CoseUnprotectedHeader unprotectedHeader;

  @JsonProperty("payload")
  byte[] payload;

  @JsonProperty("signature")
  byte[] signature;

  @JsonIgnore
  public byte[] getProtectedHeader() {
    return protectedHeader;
  }

  @JsonIgnore
  public CoseUnprotectedHeader getUnprotectedHeader() {
    return unprotectedHeader;
  }

  @JsonIgnore
  public byte[] getPayload() {
    return payload;
  }

  @JsonIgnore
  public byte[] getSignature() {
    return signature;
  }

  @JsonIgnore
  public void setProtectedHeader(byte[] protectedHeader) {
    this.protectedHeader = protectedHeader;
  }

  @JsonIgnore
  public void setUnprotectedHeader(CoseUnprotectedHeader unprotectedHeader) {
    this.unprotectedHeader = unprotectedHeader;
  }

  @JsonIgnore
  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  @JsonIgnore
  public void setSignature(byte[] signature) {
    this.signature = signature;
  }

  @Override
  public CborTags getTag() {
    return CborTags.COSE_SIGN_1;
  }
}
