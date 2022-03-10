// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.Guid;

public class GuidSerializer extends StdSerializer<Guid> {

  public GuidSerializer() {
    this(null);
  }

  public GuidSerializer(Class<Guid> t) {
    super(t);
  }

  @Override
  public void serialize(Guid guid, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeBinary(guid.toBytes());
  }
}


