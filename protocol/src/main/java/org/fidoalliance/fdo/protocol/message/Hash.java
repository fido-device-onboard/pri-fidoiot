// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"hashType", "hashValue"})
@JsonSerialize(using = GenericArraySerializer.class)
public class Hash {

  @JsonProperty("hashType")
  private HashType hashType = HashType.HMAC_SHA384;

  @JsonProperty("hashValue")
  private byte[] hashValue;

  @JsonIgnore
  public HashType getHashType() {
    return hashType;
  }

  @JsonIgnore
  public byte[] getHashValue() {
    return hashValue;
  }

  @JsonIgnore
  public void setHashType(HashType hashType) {
    this.hashType = hashType;
  }


  @JsonIgnore
  public void setHashValue(byte[] value) {
    this.hashValue = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Hash) {
      Hash hash = (Hash) obj;
      if (hash.hashType != hashType) {
        return false;
      }
      return ByteBuffer.wrap(hash.hashValue).compareTo(ByteBuffer.wrap(hashValue)) == 0;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashType, hashValue);
  }
}
