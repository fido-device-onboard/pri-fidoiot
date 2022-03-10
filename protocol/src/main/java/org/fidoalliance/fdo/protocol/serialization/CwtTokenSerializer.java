// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.CwtToken;

public class CwtTokenSerializer extends StdSerializer<CwtToken> {

  private static final int MAX_ENTRIES = 7;

  public CwtTokenSerializer() {
    this(null);
  }

  public CwtTokenSerializer(Class<CwtToken> t) {
    super(t);
  }

  @Override
  public void serialize(CwtToken value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    if (gen instanceof CBORGenerator) {
      final CBORGenerator cbg = (CBORGenerator) gen;
      cbg.writeStartObject(MAX_ENTRIES);
    } else {
      gen.writeStartObject(value, MAX_ENTRIES);
    }

    int index = 1;
    gen.writeFieldId((index++));
    gen.writeString(value.getIssuer());

    gen.writeFieldId(index++);
    gen.writeString(value.getSubject());

    gen.writeFieldId(index++);
    gen.writeString(value.getAudience());

    gen.writeFieldId(index++);
    gen.writeNumber(value.getExpiry());

    gen.writeFieldId(index++);
    gen.writeNumber(value.getNotBefore());

    gen.writeFieldId(index++);
    gen.writeNumber(value.getIssuedAt());

    gen.writeFieldId(index++);
    gen.writeBinary(value.getCwtId());

    gen.writeEndObject();
  }
}
