// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.InetAddress;
import org.fidoalliance.fdo.protocol.InvalidIpAddressException;
import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.protocol.message.RendezvousProtocol;
import org.fidoalliance.fdo.protocol.message.To2AddressEntry;
import org.fidoalliance.fdo.protocol.message.TransportProtocol;

public class To2AddressEntryDeserializer extends StdDeserializer<To2AddressEntry> {

  public To2AddressEntryDeserializer() {
    this(null);
  }

  public To2AddressEntryDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public To2AddressEntry deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    To2AddressEntry entry = new To2AddressEntry();
    int index = 0;

    JsonNode subNode = node.get(index++);
    if (subNode.isBinary()) {
      entry.setIpAddress(subNode.binaryValue());
    } else if (subNode.isTextual()) {
      entry.setIpAddress(InetAddress.getByName(subNode.textValue()).getAddress());
    } else if (subNode.isNull()) {
      entry.setIpAddress(null);
    } else {
      throw new InvalidIpAddressException(new IllegalArgumentException());
    }

    subNode = node.get(index++);
    if (subNode.isTextual()) {
      entry.setDnsAddress(subNode.textValue());
    } else if (subNode.isNull()) {
      entry.setDnsAddress(null);
    } else {
      throw new InvalidMessageException("invalid dns");
    }

    entry.setPort(node.get(index++).intValue());
    entry.setProtocol(
        TransportProtocol.fromNumber(node.get(index++).intValue())
    );

    return entry;
  }


}
