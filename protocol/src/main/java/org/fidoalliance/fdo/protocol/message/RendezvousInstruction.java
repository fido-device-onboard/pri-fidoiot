package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Optional;
import org.fidoalliance.fdo.protocol.serialization.GenericArraySerializer;
import org.fidoalliance.fdo.protocol.serialization.RendezvousInstructionDeserializer;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"variable", "value"})
@JsonSerialize(using = GenericArraySerializer.class)
@JsonDeserialize(using = RendezvousInstructionDeserializer.class)
public class RendezvousInstruction {
  @JsonProperty("variable")
  private RendezvousVariable variable = RendezvousVariable.DEV_ONLY;

  @JsonProperty("value")
  private AnyType value = AnyType.fromObject(Optional.empty());

  @JsonIgnore
  public RendezvousVariable getVariable() {
    return variable;
  }

  @JsonIgnore
  public AnyType getValue() {
    return value;
  }

  @JsonIgnore
  public void setVariable(RendezvousVariable variable) {
    this.variable = variable;
  }

  @JsonIgnore
  public void setValue(AnyType value) {
    this.value = value;
  }

}

