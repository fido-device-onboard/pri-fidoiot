package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import java.util.Map;
import org.fidoalliance.fdo.protocol.message.GenericMap;

public class GenericMapSerializer extends StdSerializer<GenericMap> {

  public GenericMapSerializer() {
    this(null);
  }

  public GenericMapSerializer(Class<GenericMap> t) {
    super(t);
  }

  @Override
  public void serialize(GenericMap value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {


    if (gen instanceof CBORGenerator) {
      CBORGenerator cbg = (CBORGenerator) gen;
      cbg.writeStartObject(value.size());
    } else {
      gen.writeStartObject(value,value.size());
    }

    for (Map.Entry<Object, Object> entry : value.entrySet()) {
      Object key = entry.getKey();
      if (key instanceof Number) {
        gen.writeFieldId(((Number) key).longValue());
      } else if (key instanceof String) {
        gen.writeFieldName(key.toString());
      }

      Object o = entry.getValue();
      gen.writeObject(o);


    }
    gen.writeEndObject();

  }
}
