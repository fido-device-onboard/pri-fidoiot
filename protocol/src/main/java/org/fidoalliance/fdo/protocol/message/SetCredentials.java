package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"voucherHeader"})
@JsonSerialize(using = GenericArraySerializer.class)
public class SetCredentials {

  @JsonProperty("voucherHeader")
  private AnyType voucherHeader;

  @JsonIgnore
  public AnyType getVoucherHeader() {
    return voucherHeader;
  }

  @JsonIgnore
  public void setVoucherHeader(AnyType voucherHeader) {
    this.voucherHeader = voucherHeader;
  }
}
