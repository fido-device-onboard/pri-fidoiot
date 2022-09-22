// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.CoseProtectedHeader;

public class CoseProtectedHeaderDeserializer extends StdDeserializer<CoseProtectedHeader> {


  private static final String ALG = "1";

  public CoseProtectedHeaderDeserializer() {
    this(null);
  }

  public CoseProtectedHeaderDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public CoseProtectedHeader deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    CoseProtectedHeader cph = new CoseProtectedHeader();

    JsonNode subNode = node.get(ALG);
    if (subNode != null) {
      cph.setAlgId(subNode.numberValue().intValue());
    }

    return cph;
  }

}
