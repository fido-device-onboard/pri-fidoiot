package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import org.fidoalliance.fdo.protocol.message.ServiceInfoQueue;

public class FdoSysModuleExtra {
  @JsonProperty("filter")
  private Map<String,String> filter = new HashMap<>();

  @JsonProperty("queue")
  private ServiceInfoQueue queue = new ServiceInfoQueue();

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
}
