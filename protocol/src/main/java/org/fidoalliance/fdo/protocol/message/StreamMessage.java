// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"length", "msgType", "protocolVersion", "protocolInfo", "body"})
@JsonSerialize(using = GenericArraySerializer.class)
public class StreamMessage {

  @JsonProperty("length")
  private int length;

  @JsonProperty("msgType")
  private MsgType msgType;

  @JsonProperty("protocolVersion")
  private ProtocolVersion protocolVersion;

  @JsonProperty("protocolInfo")
  private ProtocolInfo protocolInfo;

  @JsonProperty("body")
  private AnyType body;

  @JsonIgnore
  public int getLength() {
    return length;
  }

  @JsonIgnore
  public MsgType getMsgType() {
    return msgType;
  }

  @JsonIgnore
  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  @JsonIgnore
  public ProtocolInfo getProtocolInfo() {
    return protocolInfo;
  }

  @JsonIgnore
  public AnyType getBody() {
    return body;
  }

  @JsonIgnore
  public void setMsgType(MsgType msgType) {
    this.msgType = msgType;
  }

  @JsonIgnore
  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  @JsonIgnore
  public void setProtocolInfo(ProtocolInfo protocolInfo) {
    this.protocolInfo = protocolInfo;
  }

  @JsonIgnore
  public void setBody(AnyType body) {
    this.body = body;
  }

  /**
   * Compute the length of the message.
   * @throws IOException An error occured.
   */
  @JsonIgnore
  public void computeLength() throws IOException {

    //will encode three times to compute length
    length = getMinimumReadLength();
    for (; ; ) {
      byte[] data = Mapper.INSTANCE.writeValue(this);
      if (data.length == length) {
        break;
      }
      length = data.length;
    }
  }

  /**
   * Gets the minimum bytes to read from the stream.
   * @return The minim bytes to read.
   */
  @JsonIgnore
  public static int getMinimumReadLength() {
    return (Integer.BYTES + 1) + 1;//a5(1) + tag(1) + int32(4)
  }

  /**
   * Gets the byte remaining after reading the initial bytes.
   * @param initialBytes The buffer containing the inital bytes.
   * @return The remaining bytes of the message.
   * @throws IOException An error occurred.
   */
  @JsonIgnore
  public static int getBytesRemaining(byte[] initialBytes) throws IOException {
    byte[] numBuff = new byte[Integer.BYTES + 1];

    // the smallest stream message possible is 7 bytes T0 hello
    if (initialBytes == null || initialBytes.length < (numBuff.length + 1)) {
      throw new IOException(
          new IllegalArgumentException(
              "initial read bytes < " + (numBuff.length + 1)));
    }

    for (int i = 0; i < numBuff.length; i++) {
      numBuff[i] = initialBytes[i + 1];
    }
    // 1A 00 01 02 17
    Integer length = Mapper.INSTANCE.readValue(numBuff, Integer.class);

    return length - initialBytes.length;
  }

}
