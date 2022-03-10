// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;

public class SimpleStorageSerializer extends StdSerializer<SimpleStorage> {


  public SimpleStorageSerializer() {
    this(null);
  }

  public SimpleStorageSerializer(Class<SimpleStorage> t) {
    super(t);
  }

  @Override
  public void serialize(SimpleStorage value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    gen.writeStartObject();
    for (Object o : value.values()) {
      gen.writeFieldName(o.getClass().getName());
      gen.writeObject(o);
    }
    gen.writeEndObject();
  }
}
