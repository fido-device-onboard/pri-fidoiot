// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.fidoalliance.fdo.protocol.message.AnyType;

public class GenericArraySerializer extends StdSerializer<Object> {


  public GenericArraySerializer() {
    this(null);
  }

  public GenericArraySerializer(Class<Object> t) {
    super(t);
  }

  @Override
  public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    String[] properties = SerializerUtils.getPropertyNames(value);
    List<Object> list = new ArrayList<>(properties.length);
    for (String name : properties) {
      Object field = SerializerUtils.getPropertyValue(value, name);
      if (field == null) {
        list.add(null);
        continue;
      }
      if (field instanceof AnyType) {
        Object o = ((AnyType) field).covertValue(Object.class);
        if (o == null) {
          list.add(field);
          continue;
        }
        if (!o.equals(Optional.empty())) {
          list.add(field);
        }
      } else {
        list.add(field);
      }
    }

    if (value instanceof TaggedItem) {
      if (gen instanceof CBORGenerator) {
        CBORGenerator cbg = (CBORGenerator) gen;
        cbg.writeTag(((TaggedItem) value).getTag().toInteger());
      }
    }

    gen.writeStartArray(value, list.size());

    for (Object o : list) {
      if (o == null) {
        gen.writeNull();
      } else {
        gen.writeObject(o);

      }

    }

    gen.writeEndArray();
  }

}
