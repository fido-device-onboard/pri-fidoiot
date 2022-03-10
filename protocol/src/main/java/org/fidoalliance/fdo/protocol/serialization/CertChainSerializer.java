// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import org.fidoalliance.fdo.protocol.message.CertChain;

public class CertChainSerializer extends StdSerializer<CertChain> {

  public CertChainSerializer() {
    this(null);
  }

  public CertChainSerializer(Class<CertChain> t) {
    super(t);
  }

  @Override
  public void serialize(CertChain value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    List<Certificate> list = value.getChain();
    gen.writeStartArray(list, list.size());
    try {
      for (Certificate cert : list) {
        gen.writeBinary(cert.getEncoded());
      }
    } catch (CertificateEncodingException e) {
      throw new IOException(e);
    }
    gen.writeEndArray();
  }
}