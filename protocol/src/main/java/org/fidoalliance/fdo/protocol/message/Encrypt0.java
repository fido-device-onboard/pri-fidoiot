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
@JsonPropertyOrder({"protectedHeader", "unprotectedHeader", "cipherText"})
@JsonSerialize(using = GenericArraySerializer.class)
public class Encrypt0 implements TaggedItem {

  @JsonProperty("protectedHeader")
  private byte[] protectedHeader;

  @JsonProperty("unprotectedHeader")
  private CoseUnprotectedHeader unprotectedHeader;

  @JsonProperty("cipherText")
  private byte[] cipherText;

  @JsonIgnore
  public byte[] getProtectedHeader() {
    return protectedHeader;
  }

  @JsonIgnore
  public CoseUnprotectedHeader getUnprotectedHeader() {
    return unprotectedHeader;
  }

  @JsonIgnore
  public byte[] getCipherText() {
    return cipherText;
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
  public void setCipherText(byte[] cipherText) {
    this.cipherText = cipherText;
  }


  @Override
  public CborTags getTag() {
    return CborTags.COSE_ENCRYPT_0;
  }

}
