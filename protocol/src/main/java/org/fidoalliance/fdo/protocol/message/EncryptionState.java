// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"cipherSuite", "sek", "sev", "iv", "counter"})
@JsonSerialize(using = GenericArraySerializer.class)
public class EncryptionState {

  @JsonProperty("cipherSuite")
  private CipherSuiteType cipherSuite;

  @JsonProperty("sek")
  private byte[] sek;

  @JsonProperty("sev")
  private byte[] sev;

  @JsonProperty("iv")
  private byte[] iv;

  @JsonProperty("counter")
  private long counter;

  public CipherSuiteType getCipherSuite() {
    return cipherSuite;
  }

  public byte[] getSek() {
    return sek;
  }

  public byte[] getSev() {
    return sev;
  }

  public byte[] getIv() {
    return iv;
  }

  public long getCounter() {
    return counter;
  }

  public void setCipherSuite(CipherSuiteType cipherSuite) {
    this.cipherSuite = cipherSuite;
  }

  public void setSek(byte[] sek) {
    this.sek = sek;
  }

  public void setSev(byte[] sev) {
    this.sev = sev;
  }

  public void setIv(byte[] iv) {
    this.iv = iv;
  }

  public void setCounter(long counter) {
    this.counter = counter;
  }
}
