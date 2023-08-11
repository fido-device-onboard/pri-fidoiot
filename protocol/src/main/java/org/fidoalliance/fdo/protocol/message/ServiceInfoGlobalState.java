package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceInfoGlobalState {


  @JsonProperty("queue")
  private ServiceInfoQueue queue = new ServiceInfoQueue();

  @JsonProperty("waitQueue")
  private ServiceInfoQueue waitQueue = new ServiceInfoQueue();

  @JsonIgnore
  public ServiceInfoQueue getQueue() {
    return queue;
  }

  @JsonIgnore
  public ServiceInfoQueue getWaitQueue() {
    return waitQueue;
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
