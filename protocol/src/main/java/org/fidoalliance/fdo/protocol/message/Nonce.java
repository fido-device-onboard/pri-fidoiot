// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"nonce"})
@JsonSerialize(using = GenericArraySerializer.class)
public class Nonce {

  @JsonProperty("nonce")
  private byte[] nonce;

  @JsonIgnore
  public byte[] getNonce() {
    return nonce;
  }


  @JsonIgnore
  public void setNonce(byte[] nonce) {
    this.nonce = nonce;
  }

  @Override
  public String toString() {
    if (nonce == null) {
      return "null";
    }
    try {
      return Guid.fromBytes(nonce).toString();
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Nonce) {
      return ByteBuffer.wrap(this.nonce).compareTo(ByteBuffer.wrap(((Nonce) o).nonce)) == 0;
    }
    if (o instanceof byte[]) {
      return ByteBuffer.wrap(this.nonce).compareTo(ByteBuffer.wrap((byte[]) o)) == 0;
    }
    return super.equals(o);
  }

  public static Nonce fromRandomUUID() {
    Nonce nonce = new Nonce();
    nonce.setNonce(Guid.fromRandomUUID().toBytes());
    return nonce;
  }



}
