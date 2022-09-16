// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.CoseKey;
import org.fidoalliance.fdo.protocol.message.CoseKeyCurveType;

public class CoseKeyDeserializer extends StdDeserializer<CoseKey> {

  private static final String COSEKEY_CRV = "-1"; //from RFC8152 13.1
  private static final String COSEKEY_X = "-2"; //from RFC8152 13.1
  private static final String COSEKEY_Y = "-3"; //from RFC8152 13.1

  public CoseKeyDeserializer() {
    this(null);
  }

  public CoseKeyDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public CoseKey deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    CoseKey coseKey = new CoseKey();
    JsonNode subNode = node.get(COSEKEY_CRV);
    if (subNode.isNumber()) {
      coseKey.setCurve(CoseKeyCurveType.fromNumber(subNode.intValue()));
    } else if (subNode.isTextual()) {
      coseKey.setCurve(CoseKeyCurveType.fromString(subNode.textValue()));
    } else {
      throw new JsonParseException(jp, "no such EC key");
    }

    subNode = node.get(COSEKEY_X);
    coseKey.setX(subNode.binaryValue());

    subNode = node.get(COSEKEY_Y);
    coseKey.setY(subNode.binaryValue());

    return coseKey;
  }

}