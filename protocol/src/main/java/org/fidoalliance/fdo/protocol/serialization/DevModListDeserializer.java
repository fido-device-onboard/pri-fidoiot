// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.DevModList;

public class DevModListDeserializer extends StdDeserializer<DevModList> {

  public DevModListDeserializer() {
    this(null);
  }

  public DevModListDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public DevModList deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    DevModList modList = new DevModList();

    int index = 0;
    int storedIndex = node.get(index++).numberValue().intValue();
    int count = node.get(index++).numberValue().intValue();
    String[] names = new String[count];
    modList.setCount(count);
    modList.setIndex(storedIndex);
    modList.setModulesNames(names);
    for (int i = 0; i < count; i++) {
      names[i] = node.get(index++).textValue();
    }

    return modList;
  }

}

