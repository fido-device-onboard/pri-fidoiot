// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.LinkedList;
import java.util.List;
import org.fidoalliance.fdo.protocol.message.CertChain;


public class CertChainDeserializer extends StdDeserializer<CertChain> {

  public CertChainDeserializer() {
    this(null);
  }

  public CertChainDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public CertChain deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    List<Certificate> list = new LinkedList<>();
    CertificateFactory cf = null;
    try {
      cf = CertificateFactory.getInstance("X.509");
    } catch (CertificateException e) {
      throw new IOException(e);
    }
    for (int i = 0; i < node.size(); i++) {
      JsonNode element = node.get(i);
      try (InputStream in = new ByteArrayInputStream(element.binaryValue())) {
        list.add(cf.generateCertificate(in));
      } catch (CertificateException e) {
        throw new JsonParseException(jp, "parsing X509", e);
      }
    }

    return CertChain.fromList(list);
  }

}
