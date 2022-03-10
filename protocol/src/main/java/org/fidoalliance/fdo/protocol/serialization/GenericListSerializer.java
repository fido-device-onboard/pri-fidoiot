// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.List;

public class GenericListSerializer extends StdSerializer<List<?>> {

  public GenericListSerializer() {
    this(null);
  }

  public GenericListSerializer(Class<List<?>> t) {
    super(t);
  }

  @Override
  public void serialize(List<?> list, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartArray(list, list.size());

    for (Object o : list) {
      gen.writeObject(o);
    }

    gen.writeEndArray();
  }
}
