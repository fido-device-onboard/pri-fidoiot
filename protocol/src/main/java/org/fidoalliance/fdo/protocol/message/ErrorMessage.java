// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"errorCode", "prevMsgId", "errorString", "timestamp", "correlationId"})
@JsonSerialize(using = GenericArraySerializer.class)
public class ErrorMessage {

  @JsonProperty("errorCode")
  private ErrorCode errorCode;

  @JsonProperty("prevMsgId")
  private MsgType prevMsgId;

  @JsonProperty("errorString")
  private String errorString;

  @JsonProperty("timestamp")
  private long timestamp;

  @JsonProperty("correlationId")
  private long correlationId;

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public MsgType getPrevMsgId() {
    return prevMsgId;
  }

  public String getErrorString() {
    return errorString;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getCorrelationId() {
    return correlationId;
  }

  public void setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public void setPrevMsgId(MsgType prevMsgId) {
    this.prevMsgId = prevMsgId;
  }

  public void setErrorString(String errorString) {
    this.errorString = errorString;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setCorrelationId(long correlationId) {
    this.correlationId = correlationId;
  }
  
}
