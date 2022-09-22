// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Iterator;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;

public class SimpleStorageDeserializer extends StdDeserializer<SimpleStorage> {

  public SimpleStorageDeserializer() {
    this(null);
  }

  public SimpleStorageDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public SimpleStorage deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    SimpleStorage storage = new SimpleStorage();

    Iterator<String> names =  node.fieldNames();

    while (names.hasNext()) {
      String name = names.next();
      try {
        Class clazz = Class.forName(name);
        JsonNode subNode = node.get(name);
        Object obj = Mapper.INSTANCE.covertValue(subNode,clazz);
        storage.put(clazz,obj);

      } catch (ClassNotFoundException e) {
        throw new IOException(e);
      }

    }
    return storage;

  }

}