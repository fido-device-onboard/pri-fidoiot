package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.ManufacturingInfo;
import org.fidoalliance.fdo.protocol.message.PublicKeyEncoding;
import org.fidoalliance.fdo.protocol.message.PublicKeyType;

public class ManufacturingInfoDeserializer extends StdDeserializer<ManufacturingInfo> {

  public ManufacturingInfoDeserializer() {
    this(null);
  }

  public ManufacturingInfoDeserializer(Class<?> t) {
    super(t);
  }

  @Override
  public ManufacturingInfo deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {
    JsonNode node = jp.getCodec().readTree(jp);

    int index = 0;
    ManufacturingInfo info  = new ManufacturingInfo();

    info.setKeyType(PublicKeyType.fromNumber(node.get(index++).intValue()));
    info.setKeyEnc(PublicKeyEncoding.fromNumber(node.get(index++).intValue()));
    info.setSerialNumber(node.get(index++).textValue());
    info.setDeviceInfo(node.get(index++).textValue());
    if (index < node.size()) {
      JsonNode subNode = node.get(index++);
      if (subNode.isBinary()) {
        info.setCertInfo(AnyType.fromObject(subNode.binaryValue()));
      } else if (subNode.isObject()) {
        info.setCertInfo(AnyType.fromObject(subNode));
      }
    }
    if (index < node.size()) {
      info.setOnDieDeviceCertChain(node.get(index++).binaryValue());
    }
    if (index < node.size()) {
      info.setTestSignature(node.get(index++).binaryValue());
    }
    if (index < node.size()) {
      info.setTestSigMaroePrefix(node.get(index++).binaryValue());
    }

    return info;
  }
}
