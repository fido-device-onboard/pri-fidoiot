// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

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
  private final Object object;

  @JsonIgnore
  private AnyType(final Object object) {
    this.object = object;
  }

  @JsonIgnore
  public static AnyType fromObject(final Object object) {
    return new AnyType(object);
  }

  public Object getObject() {
    return object;
  }

  @JsonIgnore
  public <T> T covertValue(final Class<T> t) {
    JsonNode node = Mapper.INSTANCE.valueToTree(object);
    return Mapper.INSTANCE.covertValue(node, t);
  }


}
