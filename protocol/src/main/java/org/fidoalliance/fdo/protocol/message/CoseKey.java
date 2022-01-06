package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.CoseKeyDeserializer;
import org.fidoalliance.fdo.protocol.serialization.CoseKeySerializer;

/**
 * CoseKey from RFC8152.
 */

@JsonSerialize(using = CoseKeySerializer.class)
@JsonDeserialize(using = CoseKeyDeserializer.class)
public class CoseKey {

  @JsonProperty("crv")
  private CoseKeyCurveType crv;

  @JsonProperty("x")
  private byte[] x;

  @JsonProperty("y")
  private byte[] y;

  @JsonIgnore
  public CoseKeyCurveType getCrv() {
    return crv;
  }

  @JsonIgnore
  public byte[] getX() {
    return x;
  }

  @JsonIgnore
  public byte[] getY() {
    return y;
  }

  @JsonIgnore
  public void setCurve(CoseKeyCurveType crv) {
    this.crv = crv;
  }

  @JsonIgnore
  public void setX(byte[] x) {
    this.x = x;
  }

  @JsonIgnore
  public void setY(byte[] y) {
    this.y = y;
  }
}
