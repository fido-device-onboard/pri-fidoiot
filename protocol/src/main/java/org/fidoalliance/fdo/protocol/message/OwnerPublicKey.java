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
@JsonPropertyOrder({"type", "enc", "body"})
@JsonSerialize(using = GenericArraySerializer.class)
public class OwnerPublicKey {

  @JsonProperty("type")
  private PublicKeyType type = PublicKeyType.SECP384R1;

  @JsonProperty("enc")
  private PublicKeyEncoding enc = PublicKeyEncoding.X509;

  @JsonProperty("body")
  private AnyType body;

  @JsonIgnore
  public PublicKeyEncoding getEnc() {
    return enc;
  }

  @JsonIgnore
  public PublicKeyType getType() {
    return type;
  }


  @JsonIgnore
  public void setEnc(PublicKeyEncoding enc) {
    this.enc = enc;
  }

  @JsonIgnore
  public void setType(PublicKeyType type) {
    this.type = type;
  }

  @JsonIgnore
  public void setBody(AnyType body) {
    this.body = body;
  }

  @JsonIgnore
  public AnyType getBody() {
    return body;
  }
}