// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.ManufacturingInfoDeserializer;
import org.fidoalliance.fdo.protocol.serialization.ManufacturingInfoSerializer;

@JsonPropertyOrder(
    {"keyType", "keyEnc", "serialNumber", "deviceInfo", "certInfo",
        "onDieDeviceCertChain", "testSignature", "testSigMaroePrefix"}
)
@JsonSerialize(using = ManufacturingInfoSerializer.class)
@JsonDeserialize(using = ManufacturingInfoDeserializer.class)
public class ManufacturingInfo {

  @JsonProperty("keyType")
  private PublicKeyType keyType;

  @JsonProperty("keyEnc")
  private PublicKeyEncoding keyEnc;

  @JsonProperty("keyHashType")
  private HashType keyHashType;

  @JsonProperty("serialNumber")
  private String serialNumber;

  @JsonProperty("deviceInfo")
  private String deviceInfo;

  @JsonProperty("certInfo")
  private AnyType certInfo;

  @JsonProperty("onDieDeviceCertChain")
  private byte[] onDieDeviceCertChain;

  @JsonProperty("testSignature")
  private byte[] testSignature;

  @JsonProperty("testSigMaroePrefix")
  private byte[] testSigMaroePrefix;

  @JsonIgnore
  public PublicKeyType getKeyType() {
    return keyType;
  }

  @JsonIgnore
  public PublicKeyEncoding getKeyEnc() {
    return keyEnc;
  }

  @JsonIgnore
  public String getSerialNumber() {
    return serialNumber;
  }

  @JsonIgnore
  public String getDeviceInfo() {
    return deviceInfo;
  }

  @JsonIgnore
  public AnyType getCertInfo() {
    return certInfo;
  }

  @JsonIgnore
  public byte[] getOnDieDeviceCertChain() {
    return onDieDeviceCertChain;
  }

  @JsonIgnore
  public byte[] getTestSignature() {
    return testSignature;
  }

  @JsonIgnore
  public byte[] getTestSigMaroePrefix() {
    return testSigMaroePrefix;
  }

  @JsonIgnore
  public void setKeyType(PublicKeyType keyType) {
    this.keyType = keyType;
  }

  @JsonIgnore
  public void setKeyEnc(PublicKeyEncoding keyEnc) {
    this.keyEnc = keyEnc;
  }

  @JsonIgnore
  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  @JsonIgnore
  public void setDeviceInfo(String deviceInfo) {
    this.deviceInfo = deviceInfo;
  }

  @JsonIgnore
  public void setCertInfo(AnyType certInfo) {
    this.certInfo = certInfo;
  }

  @JsonIgnore
  public void setOnDieDeviceCertChain(byte[] onDieDeviceCertChain) {
    this.onDieDeviceCertChain = onDieDeviceCertChain;
  }

  @JsonIgnore
  public void setTestSignature(byte[] testSignature) {
    this.testSignature = testSignature;
  }

  @JsonIgnore
  public void setTestSigMaroePrefix(byte[] maroePrefix) {
    this.testSigMaroePrefix = maroePrefix;
  }
}
