// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.CoseUnprotectedHeader;

public class CoseUnprotectedHeaderSerializer extends StdSerializer<CoseUnprotectedHeader> {

  private static final int EAT_ETM_AES_IV = 5;
  private static final int CUPH_NONCE = 256;
  private static final int CUPH_OWNER_PUBKEY = 257;
  private static final int EAT_MAROE_PREFIX = -258;
  private static final int EAT_NONCE = -259;


  public CoseUnprotectedHeaderSerializer() {
    this(null);
  }

  public CoseUnprotectedHeaderSerializer(Class<CoseUnprotectedHeader> t) {
    super(t);
  }

  @Override
  public void serialize(CoseUnprotectedHeader value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    int count = 0;
    if (value.getCupNonce() != null) {
      count++;
    }

    if (value.getEatNonce() != null) {
      count++;
    }
    if (value.getOwnerPublicKey() != null) {
      count++;
    }

    if (value.getMaroPrefix() != null) {
      count++;
    }
    if (value.getIv() != null) {
      count++;
    }

    if (gen instanceof CBORGenerator) {
      final CBORGenerator cbg = (CBORGenerator) gen;
      cbg.writeStartObject(count);
    } else {
      gen.writeStartObject(value, count);
    }

    if (value.getIv() != null) {
      gen.writeFieldId(EAT_ETM_AES_IV);
      gen.writeBinary(value.getIv());
    }
    if (value.getCupNonce() != null) {
      gen.writeFieldId(CUPH_NONCE);
      gen.writeBinary(value.getCupNonce().getNonce());
    }

    if (value.getOwnerPublicKey() != null) {
      gen.writeFieldId(CUPH_OWNER_PUBKEY);
      gen.writeObject(value.getOwnerPublicKey());
    }

    if (value.getMaroPrefix() != null) {
      gen.writeFieldId(EAT_MAROE_PREFIX);
      gen.writeObject(value.getMaroPrefix());
    }

    if (value.getEatNonce() != null) {
      gen.writeFieldId(EAT_NONCE);
      gen.writeBinary(value.getEatNonce().getNonce());
    }
    gen.writeEndObject();

  }
}
