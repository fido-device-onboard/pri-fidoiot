// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.fidoalliance.fdo.protocol.serialization.RendezvousInstructionDeserializer;
import org.fidoalliance.fdo.protocol.serialization.RendezvousInstructionSerializer;


@JsonSerialize(using = RendezvousInstructionSerializer.class)
@JsonDeserialize(using = RendezvousInstructionDeserializer.class)
public class RendezvousInstruction {

  private RendezvousVariable variable = RendezvousVariable.DEV_ONLY;


  private byte[] value;


  public RendezvousVariable getVariable() {
    return variable;
  }


  public byte[] getValue() {
    return value;
  }


  public void setVariable(RendezvousVariable variable) {
    this.variable = variable;
  }


  public void setValue(byte[] value) {
    this.value = value;
  }

}

