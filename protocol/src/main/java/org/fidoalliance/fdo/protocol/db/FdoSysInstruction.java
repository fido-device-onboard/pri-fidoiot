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

  @JsonProperty("owner_exec")
  private String[] owner_execArgs;

  @JsonProperty("exec_cb")
  private String[] execCbArgs;

  @JsonProperty("filedesc")
  private String fileDesc;

  @JsonProperty("resource")
  private String resource;

  @JsonProperty("fetch")
  private String fetch;

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

  @JsonIgnore
  public String[] getOwnerExec() {
    return owner_execArgs;
  }

  public String getFetch() {
    return fetch;
  }
}
