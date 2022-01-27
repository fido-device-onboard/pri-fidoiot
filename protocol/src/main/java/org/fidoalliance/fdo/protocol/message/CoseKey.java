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


  private CoseKeyCurveType crv;


  private byte[] x;


  private byte[] y;


  public CoseKeyCurveType getCrv() {
    return crv;
  }


  public byte[] getX() {
    return x;
  }


  public byte[] getY() {
    return y;
  }


  public void setCurve(CoseKeyCurveType crv) {
    this.crv = crv;
  }


  public void setX(byte[] x) {
    this.x = x;
  }

  public void setY(byte[] y) {
    this.y = y;
  }
}
