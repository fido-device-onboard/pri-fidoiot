// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * The common interface for encoded (read-only) SDO messages.
 */
class EncodedProtocolMessage implements ProtocolMessage {

  private final String myBody;
  private final MessageType myType;
  private final Version myVersion;

  private EncodedProtocolMessage(Version version, MessageType messageType, String body) {
    myVersion = version;
    myType = messageType;
    myBody = body;
  }

  public static EncodedProtocolMessage getInstance(
      Version version, MessageType messageType, String body) {
    return new EncodedProtocolMessage(version, messageType, null != body ? body : "");
  }

  @Override
  public Version getVersion() {
    return myVersion;
  }

  @Override
  public MessageType getType() {
    return myType;
  }

  @Override
  public String getBody() {
    return myBody;
  }
}
