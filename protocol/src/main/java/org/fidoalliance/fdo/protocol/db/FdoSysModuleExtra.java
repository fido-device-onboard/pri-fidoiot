// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;

public class FdoSysModuleExtra {
  @JsonProperty("filter")
  private final Map<String,String> filter = new HashMap<>();

  @JsonProperty("queue")
  private ServiceInfoQueue queue = new ServiceInfoQueue();

  @JsonProperty("waitQueue")
  private ServiceInfoQueue waitQueue = new ServiceInfoQueue();

  @JsonProperty("loaded")
  private boolean loaded;

  @JsonProperty("waiting")
  private boolean waiting;


  @JsonIgnore
  public Map<String, String> getFilter() {
    return filter;
  }

  @JsonIgnore
  public ServiceInfoQueue getQueue() {
    return queue;
  }

  @JsonIgnore
  public ServiceInfoQueue getWaitQueue() {
    return waitQueue;
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
  public void setQueue(ServiceInfoQueue queue) {
    this.queue = queue;
  }

  @JsonIgnore
  public void setWaitQueue(ServiceInfoQueue waitQueue) {
    this.waitQueue = waitQueue;
  }
}
