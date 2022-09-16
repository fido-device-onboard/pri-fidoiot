// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.NullValue;

public class AnyTypeDeserializer extends StdDeserializer<AnyType> {

  public AnyTypeDeserializer() {
    this(null);
  }

  public AnyTypeDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public AnyType deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    final JsonNode node = jp.getCodec().readTree(jp);
    if (node.isNull()) {
      return AnyType.fromObject(new NullValue());
    }
    Object object = Mapper.INSTANCE.covertValue(node, Object.class);
    return AnyType.fromObject(object);
  }


}
