// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.ProtocolInfo;

public class ProtocolInfoDeserializer extends StdDeserializer<ProtocolInfo> {

  private static final String TOKEN = "token";

  public ProtocolInfoDeserializer() {
    this(null);
  }

  public ProtocolInfoDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public ProtocolInfo deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    ProtocolInfo info = new ProtocolInfo();

    if (node.has(TOKEN)) {
      JsonNode subNode = node.get(TOKEN);
      info.setAuthToken(subNode.textValue());
    }
    return info;
  }
}
