// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"mod", "exp"})
@JsonSerialize(using = GenericArraySerializer.class)
public class CryptoKey {

  @JsonProperty("mod")
  private byte[] modulus;

  @JsonProperty("exp")
  private byte[] exponent;

  public byte[] getModulus() {
    return modulus;
  }

  public byte[] getExponent() {
    return exponent;
  }

  public void setModulus(byte[] modulus) {
    this.modulus = modulus;
  }

  public void setExponent(byte[] exponent) {
    this.exponent = exponent;
  }
}
