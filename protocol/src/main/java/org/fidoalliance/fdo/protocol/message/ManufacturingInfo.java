package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.ManufacturingInfoDeserializer;
import org.fidoalliance.fdo.protocol.serialization.ManufacturingInfoSerializer;

@JsonPropertyOrder(
    {"keyType", "keyEnc", "serialNumber","deviceInfo","certInfo","testSignature"}
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

  @JsonProperty("testSignature")
  private byte[] testSignature;

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
  public byte[] getTestSignature() {
    return testSignature;
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
  public void setTestSignature(byte[] testSignature) {
    this.testSignature = testSignature;
  }
}
