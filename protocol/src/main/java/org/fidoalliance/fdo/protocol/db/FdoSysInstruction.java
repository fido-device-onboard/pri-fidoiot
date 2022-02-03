package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class FdoSysInstruction {

  @JsonProperty("filter")
  private Map<String,String> filter;

  @JsonProperty("exec")
  private String[] execArgs;

  @JsonProperty("filedesc")
  private String fileDesc;

  @JsonProperty("resource")
  private String resource;

  @JsonIgnore
  public Map<String, String> getFilter() {
    return filter;
  }

  @JsonIgnore
  public String[] getExecArgs() {
    return execArgs;
  }

  @JsonIgnore
  public String getFileDesc() {
    return fileDesc;
  }

  @JsonIgnore
  public String getResource() {
    return resource;
  }

}
