// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;

public class ManufacturingInfoSerializer extends StdSerializer<ManufacturingInfo> {

  public ManufacturingInfoSerializer() {
    this(null);
  }

  public ManufacturingInfoSerializer(Class<ManufacturingInfo> t) {
    super(t);
  }

  @Override
  public void serialize(ManufacturingInfo value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    String[] properties = SerializerUtils.getPropertyNames(value);
    List<Object> list = new ArrayList<>(properties.length);

    //get all non-null values
    for (String name : properties) {
      Object field = SerializerUtils.getPropertyValue(value, name);
      if (field == null) {
        continue;
      }
      list.add(field);
    }

    gen.writeStartArray(value, list.size());

    for (Object o : list) {
      gen.writeObject(o);
    }

    gen.writeEndArray();
  }

}
