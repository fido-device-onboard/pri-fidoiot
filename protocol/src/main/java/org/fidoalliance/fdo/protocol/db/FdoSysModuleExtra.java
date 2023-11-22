// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import org.fidoalliance.fdo.protocol.message.ServiceInfoGlobalState;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;

public class FdoSysModuleExtra {

  @JsonProperty("filter")
  private final Map<String, String> filter = new HashMap<>();

  @JsonProperty("loaded")
  private boolean loaded;

  @JsonProperty("waiting")
  private boolean waiting;

  @JsonProperty("length")
  private int length;

  @JsonProperty("name")
  private String name;

  @JsonProperty("checksum")
  private byte[] checksum;

  @JsonProperty("data")
  private byte[] data;

  @JsonProperty("received")
  private int received;

  @JsonIgnore
  public Map<String, String> getFilter() {
    return filter;
  }


  @JsonIgnore
  public int getLength() {
    return length;
  }

  @JsonIgnore
  public String getName() {
    return name;
  }

  @JsonIgnore
  public byte[] getChecksum() {
    return checksum;
  }

  @JsonIgnore
  public byte[] getData() {
    return data;
  }

  @JsonIgnore
  public int getReceived() {
    return received;
  }


  @JsonIgnore
  public boolean isLoaded() {
    return loaded;
  }

  @JsonIgnore
  public boolean isWaiting() {
    return waiting;
  }

  @JsonIgnore
  public void setLoaded(boolean loaded) {
    this.loaded = loaded;
  }

  @JsonIgnore
  public void setWaiting(boolean waiting) {
    this.waiting = waiting;
  }


  @JsonIgnore
  public void setLength(int length) {
    this.length = length;
  }

  @JsonIgnore
  public void setName(String name) {
    this.name = name;
  }

  @JsonIgnore
  public void setChecksum(byte[] checksum) {
    this.checksum = checksum;
  }

  @JsonIgnore
  public void setData(byte[] data) {
    this.data = data;
  }

  @JsonIgnore
  public void setReceived(int received) {
    this.received = received;
  }

}
