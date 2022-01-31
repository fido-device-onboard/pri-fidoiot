package org.fidoalliance.fdo.sample;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RootConfig {

  @JsonProperty("device")
  private DeviceConfig config;

  public DeviceConfig getRoot() {
    return config;
  }
}
