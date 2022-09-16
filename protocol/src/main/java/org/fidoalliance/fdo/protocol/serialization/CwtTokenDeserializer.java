// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.CwtToken;


public class CwtTokenDeserializer extends StdDeserializer<CwtToken> {

  public CwtTokenDeserializer() {
    this(null);
  }

  public CwtTokenDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public CwtToken deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {

    JsonNode node = jp.getCodec().readTree(jp);
    CwtToken cwtToken = new CwtToken();

    int index = 1;
    cwtToken.setIssuer(node.get(Integer.toString(index++)).textValue());
    cwtToken.setSubject(node.get(Integer.toString(index++)).textValue());
    cwtToken.setAudience(node.get(Integer.toString(index++)).textValue());
    cwtToken.setExpiry(node.get(Integer.toString(index++)).numberValue().longValue());
    cwtToken.setNotBefore(node.get(Integer.toString(index++)).numberValue().longValue());
    cwtToken.setIssuedAt(node.get(Integer.toString(index++)).longValue());
    cwtToken.setCwtId(node.get(Integer.toString(index++)).binaryValue());

    return cwtToken;
  }

}
