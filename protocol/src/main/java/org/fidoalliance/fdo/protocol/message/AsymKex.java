package org.fidoalliance.fdo.protocol.message;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"randomSize","b" ,"party"} )
public class AsymKex {

  @JsonProperty("randomSize")
  private int randomSize;

  @JsonProperty("b")
  private byte[] b;

  @JsonProperty("party")
  KexParty party;

  public int getRandomSize() {
    return randomSize;
  }

  public byte[] getB() {
    return b;
  }

  public KexParty getParty() {
    return party;
  }

  public void setRandomSize(int randomSize) {
    this.randomSize = randomSize;
  }

  public void setB(byte[] b) {
    this.b = b;
  }

  public void setParty(KexParty party) {
    this.party = party;
  }
}
