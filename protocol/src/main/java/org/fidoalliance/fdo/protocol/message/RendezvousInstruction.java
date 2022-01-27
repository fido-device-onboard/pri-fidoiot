package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.RendezvousInstructionDeserializer;
import org.fidoalliance.fdo.protocol.serialization.RendezvousInstructionSerializer;


@JsonSerialize(using = RendezvousInstructionSerializer.class)
@JsonDeserialize(using = RendezvousInstructionDeserializer.class)
public class RendezvousInstruction {

  private RendezvousVariable variable = RendezvousVariable.DEV_ONLY;


  private AnyType value;


  public RendezvousVariable getVariable() {
    return variable;
  }


  public AnyType getValue() {
    return value;
  }


  public void setVariable(RendezvousVariable variable) {
    this.variable = variable;
  }


  public void setValue(AnyType value) {
    this.value = value;
  }

}

