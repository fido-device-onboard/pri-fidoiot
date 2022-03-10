// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.ProtocolInfo;


public class ProtocolInfoSerializer extends StdSerializer<ProtocolInfo> {

  private static final String TOKEN = "token";

  public ProtocolInfoSerializer() {
    this(null);
  }

  public ProtocolInfoSerializer(Class<ProtocolInfo> t) {
    super(t);
  }

  @Override
  public void serialize(ProtocolInfo value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    int itemCount = 0;
    if (value.getAuthToken() != null) {
      itemCount++;
    }
    if (gen instanceof CBORGenerator) {
      final CBORGenerator cbg = (CBORGenerator) gen;
      cbg.writeStartObject(itemCount);
    } else {
      gen.writeStartObject(value, itemCount);
    }
    if (itemCount > 0) {
      gen.writeFieldName(TOKEN);
      gen.writeString(value.getAuthToken());
    }

    gen.writeEndObject();
  }
}
