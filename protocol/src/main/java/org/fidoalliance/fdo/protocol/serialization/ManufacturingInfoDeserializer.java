// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.CertChain;
import org.fidoalliance.fdo.protocol.message.HashType;
import org.fidoalliance.fdo.protocol.message.KeySizeType;
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

  /**
   * Checks whether string contains only alphanumberic characters along with '_' and '-'.
   * @param deviceString The string to be validated as a device string.
   * @return True if the string is a valid device string, false otherwise.
   */
  public boolean isValidString(String deviceString) {
    String[] invalidStrings = { "", "null", "none", "true", "false", "undefined", "undef",
        "NaN", "nil",
        "_", "-", "--", "--version", "--help" };

    Boolean invalidStringMatch = Arrays.stream(invalidStrings).anyMatch(
        invalid -> invalid.equalsIgnoreCase(deviceString));

    Boolean invalidCharMatch = deviceString.chars().anyMatch(
        c -> !Character.isLetterOrDigit(c) && c != '_' && c != '-');

    return !invalidStringMatch && !invalidCharMatch;
  }

  @Override
  public ManufacturingInfo deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    
    int index = 0;
    ManufacturingInfo info = new ManufacturingInfo();
    info.setKeyType(PublicKeyType.fromNumber(node.get(index++).intValue()));
    info.setKeyEnc(PublicKeyEncoding.fromNumber(node.get(index++).intValue()));

    String serialNumber = node.get(index++).textValue();

    if (isValidString(serialNumber)) {
      info.setSerialNumber(serialNumber);
    } else {
      throw new InvalidMessageException("Invalid Serial Number");
    }

    String deviceInfo = node.get(index++).textValue();

    if (isValidString(deviceInfo) || deviceInfo.isEmpty()) {
      info.setDeviceInfo(deviceInfo);
    } else {
      throw new InvalidMessageException("Invalid Device Info");
    }

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
