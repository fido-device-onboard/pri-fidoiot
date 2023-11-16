// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import org.fidoalliance.fdo.protocol.serialization.NonceDeserializer;
import org.fidoalliance.fdo.protocol.serialization.NonceSerializer;


@JsonPropertyOrder({"nonce"})
@JsonSerialize(using = NonceSerializer.class)
@JsonDeserialize(using = NonceDeserializer.class)
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
    if (this == o) {
      return true;
    }
    if (o instanceof Nonce) {
      return ByteBuffer.wrap(this.nonce).compareTo(ByteBuffer.wrap(((Nonce) o).nonce)) == 0;
    }
    if (o instanceof byte[]) {
      return Arrays.equals(this.nonce, (byte[]) o);
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nonce);
  }

  /**
   * Gets a nonce from a random 16 byte value.
   * @return The nonce.
   */
  @JsonIgnore
  public static Nonce fromRandomUuid() {
    Nonce nonce = new Nonce();
    nonce.setNonce(Guid.fromRandomUuid().toBytes());
    return nonce;
  }



}
