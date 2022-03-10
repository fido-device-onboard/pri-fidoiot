// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;
import org.fidoalliance.fdo.protocol.serialization.To2AddressEntryDeserializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"ipAddress", "dnsAddress", "port", "protocol"})
@JsonSerialize(using = GenericArraySerializer.class)
@JsonDeserialize(using = To2AddressEntryDeserializer.class)
public class To2AddressEntry {

  @JsonProperty("ipAddress")
  private byte[] ipAddress;

  @JsonProperty("dnsAddress")
  private String dnsAddress;

  @JsonProperty("port")
  private int port;

  @JsonProperty("protocol")
  private TransportProtocol protocol;

  @JsonIgnore
  public byte[] getIpAddress() {
    return ipAddress;
  }

  @JsonIgnore
  public String getDnsAddress() {
    return dnsAddress;
  }

  @JsonIgnore
  public int getPort() {
    return port;
  }

  @JsonIgnore
  public TransportProtocol getProtocol() {
    return protocol;
  }

  @JsonIgnore
  public void setIpAddress(byte[] ipAddress) {
    this.ipAddress = ipAddress;
  }

  @JsonIgnore
  public void setDnsAddress(String dnsAddress) {
    this.dnsAddress = dnsAddress;
  }

  @JsonIgnore
  public void setPort(int port) {
    this.port = port;
  }

  @JsonIgnore
  public void setProtocol(TransportProtocol protocol) {
    this.protocol = protocol;
  }
}
