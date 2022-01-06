package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;



@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"protectedHeader", "unprotectedHeader", "payload", "signature"})
@JsonSerialize(using = GenericArraySerializer.class)
public class CoseItem {

  @JsonProperty("protectedHeader")
  byte[] protectedHeader;

  @JsonProperty("unprotectedHeader")
  GenericMap unprotectedHeader;

  @JsonProperty("payload")
  byte[] payload;

  @JsonProperty("signature")
  byte[] signature;

  @JsonIgnore
  public byte[] getProtectedHeader() {
    return protectedHeader;
  }

  @JsonIgnore
  public GenericMap getUnprotectedHeader() {
    return unprotectedHeader;
  }

  @JsonIgnore
  public byte[] getPayload() {
    return payload;
  }

  @JsonIgnore
  public byte[] getSignature() {
    return signature;
  }

  @JsonIgnore
  public void setProtectedHeader(byte[] protectedHeader) {
    this.protectedHeader = protectedHeader;
  }

  @JsonIgnore
  public void setUnprotectedHeader(GenericMap unprotectedHeader) {
    this.unprotectedHeader = unprotectedHeader;
  }

  @JsonIgnore
  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  @JsonIgnore
  public void setSignature(byte[] signature) {
    this.signature = signature;
  }


}
