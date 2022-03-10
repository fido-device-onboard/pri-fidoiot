// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.AnyType;

public class AnyTypeSerializer extends StdSerializer<AnyType> {


  public AnyTypeSerializer() {
    this(null);
  }

  public AnyTypeSerializer(Class<AnyType> t) {
    super(t);
  }

  @Override
  public void serialize(AnyType value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeObject(value.getObject());
  }
}
