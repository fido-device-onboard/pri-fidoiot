package org.fidoalliance.fdo.protocol.message;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"manufacturingInfo"})
@JsonSerialize(using = GenericArraySerializer.class)
public class AppStart {

  @JsonProperty("manufacturingInfo")
  private AnyType manufacturingInfo;

  @JsonIgnore
  public AnyType getManufacturingInfo() {
    return manufacturingInfo;
  }

  @JsonIgnore
  public void setManufacturingInfo(AnyType mfgInfo) {
    this.manufacturingInfo = mfgInfo;
  }
}
