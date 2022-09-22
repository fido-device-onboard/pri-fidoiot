// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.Nonce;

public class NonceDeserializer extends StdDeserializer<Nonce> {

  public NonceDeserializer() {
    this(null);
  }

  public NonceDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public Nonce deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    Nonce nonce = new Nonce();
    nonce.setNonce(node.binaryValue());
    return nonce;
  }

}
