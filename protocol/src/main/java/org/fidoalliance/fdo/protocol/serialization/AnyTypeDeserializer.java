package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.message.AnyType;

public class AnyTypeDeserializer extends StdDeserializer<AnyType> {

  public AnyTypeDeserializer() {
    this(null);
  }

  public AnyTypeDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public AnyType deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    final JsonNode node = jp.getCodec().readTree(jp);
    return AnyType.fromObject(node);
  }


}
