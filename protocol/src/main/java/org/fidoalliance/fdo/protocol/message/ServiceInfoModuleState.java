// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"name", "active", "active_sent", "more", "done", "mtu", "guid", "extra"})
public class ServiceInfoModuleState {

  @JsonProperty("name")
  private String name;

  @JsonProperty("active")
  private boolean active;

  @JsonProperty("active_sent")
  private boolean activeSent;

  @JsonProperty("more")
  private boolean more;

  @JsonProperty("done")
  private boolean done;

  @JsonProperty("mtu")
  private int mtu;

  @JsonProperty("guid")
  private Guid guid;

  @JsonProperty("extra")
  private AnyType extra;

  @JsonIgnore
  private ServiceInfoGlobalState globalState;


  @JsonIgnore
  private ServiceInfoDocument document;

  @JsonIgnore
  public String getName() {
    return name;
  }

  @JsonIgnore
  public boolean isActive() {
    return active;
  }

  @JsonIgnore
  public boolean isDone() {
    return done;
  }

  @JsonIgnore
  public boolean isMore() {
    return more;
  }

  @JsonIgnore
  public int getMtu() {
    return mtu;
  }

  @JsonIgnore
  public Guid getGuid() {
    return guid;
  }

  @JsonIgnore
  public AnyType getExtra() {
    return extra;
  }

  @JsonIgnore
  public ServiceInfoDocument getDocument() {
    return document;
  }

  @JsonIgnore
  public boolean getActiveSent() {
    return activeSent;
  }

  @JsonIgnore
  public ServiceInfoGlobalState getGlobalState() {
    return this.globalState;
  }

  @JsonIgnore
  public void setName(String name) {
    this.name = name;
  }

  @JsonIgnore
  public void setActive(boolean active) {
    this.active = active;
  }

  @JsonIgnore
  public void setMore(boolean more) {
    this.more = more;
  }

  @JsonIgnore
  public void setDone(boolean done) {
    this.done = done;
  }

  @JsonIgnore
  public void setMtu(int mtu) {
    this.mtu = mtu;
  }

  @JsonIgnore
  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  @JsonIgnore
  public void setExtra(AnyType extra) {
    this.extra = extra;
  }

  @JsonIgnore
  public void setDocument(ServiceInfoDocument document) {
    this.document = document;
  }

  @JsonIgnore
  public void setGlobalState(ServiceInfoGlobalState state) {
    this.globalState = state;
  }

  @JsonIgnore
  public void setActiveSent(boolean sent) {
    this.activeSent = sent;
  }

}
