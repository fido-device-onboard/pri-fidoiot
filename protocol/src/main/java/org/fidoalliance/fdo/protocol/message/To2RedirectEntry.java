package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"to1d","pubKey"})
@JsonSerialize(using = GenericArraySerializer.class)
public class To2RedirectEntry {
  @JsonProperty("to1d")
  private CoseSign1 to1d;
  @JsonProperty("pubKey")
  private OwnerPublicKey publicKey;

  @JsonIgnore
  public CoseSign1 getTo1d() {
    return to1d;
  }

  @JsonIgnore
  public OwnerPublicKey getPublicKey() {
    return publicKey;
  }

  @JsonIgnore
  public void setTo1d(CoseSign1 to1d) {
    this.to1d = to1d;
  }

  @JsonIgnore
  public void setPublicKey(OwnerPublicKey publicKey) {
    this.publicKey = publicKey;
  }
}
