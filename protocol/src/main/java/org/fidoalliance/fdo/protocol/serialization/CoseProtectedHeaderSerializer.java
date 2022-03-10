// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.CoseProtectedHeader;

public class CoseProtectedHeaderSerializer extends StdSerializer<CoseProtectedHeader> {

  static final int ALG = 1;


  public CoseProtectedHeaderSerializer() {
    this(null);
  }

  public CoseProtectedHeaderSerializer(Class<CoseProtectedHeader> t) {
    super(t);
  }

  @Override
  public void serialize(CoseProtectedHeader value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    if (gen instanceof CBORGenerator) {
      final CBORGenerator cbg = (CBORGenerator) gen;
      cbg.writeStartObject(1);
    } else {
      gen.writeStartObject(value, 1);
    }

    gen.writeFieldId(ALG);
    gen.writeNumber(value.getAlgId());

    gen.writeEndObject();

  }
}

