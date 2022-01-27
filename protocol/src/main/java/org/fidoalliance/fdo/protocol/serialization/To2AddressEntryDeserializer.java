package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.InetAddress;
import org.fidoalliance.fdo.protocol.InvalidIpAddressException;
import org.fidoalliance.fdo.protocol.message.RendezvousProtocol;
import org.fidoalliance.fdo.protocol.message.To2AddressEntry;

public class To2AddressEntryDeserializer extends StdDeserializer<To2AddressEntry> {

  public To2AddressEntryDeserializer() {
    this(null);
  }

  public To2AddressEntryDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public To2AddressEntry deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    JsonNode node = jp.getCodec().readTree(jp);

    To2AddressEntry entry = new To2AddressEntry();
    int index = 0;


    JsonNode subNode = node.get(index++);
    if (subNode.isBinary()) {
      entry.setIpAddress(subNode.binaryValue());
    } else if (subNode.isTextual()) {
      entry.setIpAddress(InetAddress.getByName(subNode.textValue()).getAddress());
    } else {
      throw new InvalidIpAddressException(new IllegalArgumentException());
    }

    entry.setDnsAddress(node.get(index++).textValue());

    entry.setPort(node.get(index++).intValue());
    entry.setProtocol(
        RendezvousProtocol.fromNumber(node.get(index++).intValue())
    );

    return entry;
  }


}
