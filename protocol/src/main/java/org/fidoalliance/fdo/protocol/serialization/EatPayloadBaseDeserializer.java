// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.EatPayloadBase;
import org.fidoalliance.fdo.protocol.message.Guid;
import org.fidoalliance.fdo.protocol.message.Nonce;

public class EatPayloadBaseDeserializer extends StdDeserializer<EatPayloadBase> {

  private static final String EAT_NONCE = "10";
  private static final String EAT_UEID = "256";
  private static final String EAT_FDO = "-257";

  public EatPayloadBaseDeserializer() {
    this(null);
  }

  public EatPayloadBaseDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public EatPayloadBase deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    EatPayloadBase eat = new EatPayloadBase();
    JsonNode subNode = node.get(EAT_NONCE);
    if (subNode != null) {
      Nonce nonce = new Nonce();
      nonce.setNonce(subNode.binaryValue());
      eat.setNonce(nonce);
    }

    subNode = node.get(EAT_UEID);
    if (subNode != null) {
      byte[] eatData = subNode.binaryValue();
      if (eatData.length < 1 || eatData[0] != 1) {
        throw new InvalidMessageException(new IllegalArgumentException());
      }
      byte[] guidBytes = new byte[eatData.length - 1];
      System.arraycopy(eatData, 1, guidBytes, 0, guidBytes.length);

      eat.setGuid(Guid.fromBytes(guidBytes));
    }

    subNode = node.get(EAT_FDO);
    if (subNode != null) {
      eat.setFdoClaim(AnyType.fromObject(subNode));
    }

    return eat;
  }

}
