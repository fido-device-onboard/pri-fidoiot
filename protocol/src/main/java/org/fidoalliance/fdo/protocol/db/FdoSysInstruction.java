// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class FdoSysInstruction {

  @JsonProperty("filter")
  private Map<String,String> filter;

  @JsonProperty("exec")
  private String[] execArgs;

  @JsonProperty("exec_cb")
  private String[] execCbArgs;

  @JsonProperty("filedesc")
  private String fileDesc;

  @JsonProperty("resource")
  private String resource;

  @JsonProperty("fetch")
  private String fetch;

  @JsonProperty("url")
  private String url;

  @JsonProperty("sha")
  private String sha;

  @JsonIgnore
  public Map<String, String> getFilter() {
    return filter;
  }

  @JsonIgnore
  public String[] getExecArgs() {
    return execArgs;
  }

  @JsonIgnore
  public String[] getExecCbArgs() {
    return execCbArgs;
  }

  @JsonIgnore
  public String getFileDesc() {
    return fileDesc;
  }

  @JsonIgnore
  public String getResource() {
    return resource;
  }

  public String getFetch() {
    return fetch;
  }

  @JsonIgnore
  public String getUrl() {
    return url;
  }

  @JsonIgnore
  public String getSha() {
    return sha;
  }
}
