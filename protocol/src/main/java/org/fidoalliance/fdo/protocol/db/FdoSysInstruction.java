// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class FdoSysInstruction {

  @JsonProperty("filter")
  private Map<String, String> filter;

  @JsonProperty("exec")
  private String[] execArgs;

  @JsonProperty("owner_exec")
  private String[] ownerExecArgs;

  @JsonProperty("exec_cb")
  private String[] execCbArgs;

  @JsonProperty("filedesc")
  private String fileDesc;

  @JsonProperty("resource")
  private String resource;

  @JsonProperty("fetch")
  private String fetch;

  @JsonProperty("wget")
  private String webGet;

  @JsonProperty("sha384")
  private String sha384;

  @JsonProperty("name")
  private String name;

  @JsonProperty("may_fail")
  private String mayFail;

  @JsonProperty("return_stdout")
  private String returnStdOut;

  @JsonProperty("return_stderr")
  private String returnStdErr;

  @JsonProperty("module")
  private String module;


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
    return ownerExecArgs;
  }

  public String getFetch() {
    return fetch;
  }

  @JsonIgnore
  public String getWebGet() {
    return webGet;
  }

  @JsonIgnore
  public String getSha384() {
    return sha384;
  }

  @JsonIgnore
  public String getName() {
    return name;
  }

  @JsonIgnore
  public String getMayFail() {
    return mayFail;
  }

  @JsonIgnore
  public String getReturnStdOut() {
    return returnStdOut;
  }

  @JsonIgnore
  public String getReturnStdErr() {
    return returnStdErr;
  }

  @JsonIgnore
  public String getModule() {
    return module;
  }
}
