package org.fidoalliance.fdo.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.serialization.AnyTypeDeserializer;
import org.fidoalliance.fdo.protocol.serialization.AnyTypeSerializer;

@JsonSerialize(using = AnyTypeSerializer.class)
@JsonDeserialize(using = AnyTypeDeserializer.class)
public class AnyType {

  @JsonIgnore
  private JsonNode node;

  @JsonIgnore
  private AnyType(final Object object) {
    if (object instanceof JsonNode) {
      this.node = (JsonNode) object;
    } else {
      this.node = Mapper.INSTANCE.valueToTree(object);
    }
  }

  @JsonIgnore
  public static AnyType fromObject(final Object object) {
    return new AnyType(object);
  }

  @JsonIgnore
  public void wrap() throws IOException {
    Object o = covertValue(Object.class);
    byte[] data = Mapper.INSTANCE.writeValue(o);
    this.node =  Mapper.INSTANCE.valueToTree(data);
  }

  @JsonIgnore
  public <T> T unwrap(final Class<T> t) throws IOException {
    byte[] wrapped = Mapper.INSTANCE.covertValue(node, byte[].class);
    return Mapper.INSTANCE.readValue(wrapped, t);

  }


  @JsonIgnore
  public <T> T covertValue(final Class<T> t) {
    return Mapper.INSTANCE.covertValue(node, t);
  }


}
