// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.DevModList;

public class DevModListSerializer extends StdSerializer<DevModList> {

  public DevModListSerializer() {
    this(null);
  }

  public DevModListSerializer(Class<DevModList> t) {
    super(t);
  }

  @Override
  public void serialize(DevModList value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    gen.writeStartArray(value, 2 + value.getCount());

    gen.writeNumber(value.getIndex());
    gen.writeNumber(value.getCount());
    for (String name : value.getModulesNames()) {
      gen.writeString(name);
    }

    gen.writeEndArray();

  }


}
