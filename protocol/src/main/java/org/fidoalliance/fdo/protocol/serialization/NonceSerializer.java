// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.Nonce;

public class NonceSerializer extends StdSerializer<Nonce> {

  public NonceSerializer() {
    this(null);
  }

  public NonceSerializer(Class<Nonce> t) {
    super(t);
  }

  @Override
  public void serialize(Nonce value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeBinary(value.getNonce());
  }
}


