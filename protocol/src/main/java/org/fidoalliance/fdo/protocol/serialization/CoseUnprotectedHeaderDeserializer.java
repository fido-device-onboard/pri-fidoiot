// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.message.CoseUnprotectedHeader;
import org.fidoalliance.fdo.protocol.message.EatPayloadBase;
import org.fidoalliance.fdo.protocol.message.Nonce;
import org.fidoalliance.fdo.protocol.message.OwnerPublicKey;

public class CoseUnprotectedHeaderDeserializer extends StdDeserializer<CoseUnprotectedHeader> {


  private static final String EAT_ETM_AES_IV = "5";
  private static final String CUPH_NONCE = "256";
  private static final String CUPH_OWNER_PUBKEY = "257";
  private static final String EAT_MAROE_PREFIX = "-258";
  private static final String EAT_UPH_NONCE = "-259";


  public CoseUnprotectedHeaderDeserializer() {
    this(null);
  }

  public CoseUnprotectedHeaderDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public CoseUnprotectedHeader deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    CoseUnprotectedHeader uph = new CoseUnprotectedHeader();

    JsonNode subNode = node.get(EAT_ETM_AES_IV);
    if (subNode != null) {
      uph.setIv(subNode.binaryValue());
    }

    subNode = node.get(CUPH_NONCE);
    if (subNode != null) {
      Nonce nonce = new Nonce();
      nonce.setNonce(subNode.binaryValue());
      uph.setCupNonce(nonce);
    }

    subNode = node.get(CUPH_OWNER_PUBKEY);
    if (subNode != null) {
      OwnerPublicKey ownerPublicKey = Mapper.INSTANCE.covertValue(subNode, OwnerPublicKey.class);
      uph.setOwnerPublicKey(ownerPublicKey);
    }

    subNode = node.get(EAT_MAROE_PREFIX);
    if (subNode != null) {
      uph.setMaroPrefix(subNode.binaryValue());
    }

    subNode = node.get(EAT_UPH_NONCE);
    if (subNode != null) {
      Nonce nonce = new Nonce();
      nonce.setNonce(subNode.binaryValue());
      uph.setEatNonce(nonce);
    }

    return uph;
  }
}
