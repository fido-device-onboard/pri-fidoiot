// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;
import org.fidoalliance.fdo.protocol.serialization.TaggedItem;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"protectedHeader", "unprotectedHeader", "payload", "tagValue"})
@JsonSerialize(using = GenericArraySerializer.class)
public class Mac0 implements TaggedItem {

  @JsonProperty("protectedHeader")
  private byte[] protectedHeader;

  @JsonProperty("unprotectedHeader")
  private CoseUnprotectedHeader unprotectedHeader;

  @JsonProperty("payload")
  private byte[] payload;

  @JsonProperty("tagValue")
  private byte[] tagValue;

  public byte[] getProtectedHeader() {
    return protectedHeader;
  }

  public CoseUnprotectedHeader getUnprotectedHeader() {
    return unprotectedHeader;
  }

  public byte[] getPayload() {
    return payload;
  }

  public byte[] getTagValue() {
    return tagValue;
  }

  public void setProtectedHeader(byte[] protectedHeader) {
    this.protectedHeader = protectedHeader;
  }

  public void setUnprotectedHeader(CoseUnprotectedHeader unprotectedHeader) {
    this.unprotectedHeader = unprotectedHeader;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public void setTagValue(byte[] tagValue) {
    this.tagValue = tagValue;
  }

  @Override
  public CborTags getTag() {
    return CborTags.COSE_MAC_0;
  }
}
