// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.EatPayloadBase;

public class EatPayloadBaseSerializer extends StdSerializer<EatPayloadBase> {


  private static final int EAT_NONCE = 10;
  private static final int EAT_UEID = 256;
  private static final int EAT_RAND = 1;
  private static final int EAT_FDO = -257;

  public EatPayloadBaseSerializer() {
    this(null);
  }

  public EatPayloadBaseSerializer(Class<EatPayloadBase> t) {
    super(t);
  }

  @Override
  public void serialize(EatPayloadBase value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    int count = 0;
    if (value.getFdoClaim() != null) {
      count++;
    }
    if (value.getNonce() != null) {
      count++;
    }
    if (value.getGuid() != null) {
      count++;
    }

    if (gen instanceof CBORGenerator) {
      final CBORGenerator cbg = (CBORGenerator) gen;
      cbg.writeStartObject(count);
    } else {
      gen.writeStartObject(value, count);
    }

    if (value.getNonce() != null) {
      //write the curve
      gen.writeFieldId(EAT_NONCE);
      gen.writeBinary(value.getNonce().getNonce());
    }

    if (value.getGuid() != null) {
      gen.writeFieldId(EAT_UEID);

      byte[] guidBytes = value.getGuid().toBytes();
      byte[] eatData = new byte[guidBytes.length + 1];
      eatData[0] = EAT_RAND;
      System.arraycopy(guidBytes, 0, eatData, 1, guidBytes.length);
      gen.writeBinary(eatData);
    }

    if (value.getFdoClaim() != null) {
      gen.writeFieldId(EAT_FDO);
      gen.writeObject(value.getFdoClaim());
    }

    gen.writeEndObject();
  }

}
