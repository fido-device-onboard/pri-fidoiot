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
@JsonPropertyOrder({"header", "numEntries", "hmac", "nonce", "sigInfoB", "kexA", "helloHash",
    "maxMessageSize"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To2ProveHeaderPayload {

  @JsonProperty("header")
  private byte[] header;

  @JsonProperty("numEntries")
  private int numEntries;

  @JsonProperty("hmac")
  private Hash hmac;

  @JsonProperty("nonce")
  private Nonce nonce;

  @JsonProperty("sigInfoB")
  private SigInfo sigInfoB;

  @JsonProperty("kexA")
  private byte[] kexA;

  @JsonProperty("helloHash")
  private Hash helloHash;

  @JsonProperty("maxMessageSize")
  private int maxMessageSize;

  @JsonIgnore
  public byte[] getHeader() {
    return header;
  }

  @JsonIgnore
  public int getNumEntries() {
    return numEntries;
  }

  @JsonIgnore
  public Hash getHmac() {
    return hmac;
  }

  @JsonIgnore
  public Nonce getNonce() {
    return nonce;
  }

  @JsonIgnore
  public SigInfo getSigInfoB() {
    return sigInfoB;
  }

  @JsonIgnore
  public byte[] getKexA() {
    return kexA;
  }

  @JsonIgnore
  public Hash getHelloHash() {
    return helloHash;
  }

  @JsonIgnore
  public int getMaxMessageSize() {
    return maxMessageSize;
  }

  @JsonIgnore
  public void setHeader(byte[] header) {
    this.header = header;
  }

  @JsonIgnore
  public void setNumEntries(int numEntries) {
    this.numEntries = numEntries;
  }

  @JsonIgnore
  public void setHmac(Hash hmac) {
    this.hmac = hmac;
  }

  @JsonIgnore
  public void setNonce(Nonce nonce) {
    this.nonce = nonce;
  }

  @JsonIgnore
  public void setSigInfoB(SigInfo sigInfoB) {
    this.sigInfoB = sigInfoB;
  }

  @JsonIgnore
  public void setKexA(byte[] kexA) {
    this.kexA = kexA;
  }

  @JsonIgnore
  public void setHelloHash(Hash helloHash) {
    this.helloHash = helloHash;
  }

  @JsonIgnore
  public void setMaxMessageSize(int maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }
}
