// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PM.DeviceCredentials113.
 *
 * @see "SDO Protocol Specification, 1.12k, 5.2.6: PM.DeviceCredentials113"
 */
class DeviceCredentials113 implements DeviceCredentials, ProtocolMessage {

  private static Logger myLogger = LoggerFactory.getLogger(DeviceCredentials113.class);
  private final DeviceState deviceState;
  private final ManufacturerBlock manufacturerBlock;
  private final OwnerBlock ownerBlock;
  private final byte[] secret;

  /**
   * Constructor.
   */
  DeviceCredentials113(
      DeviceState st,
      byte[] secret,
      ManufacturerBlock m,
      OwnerBlock o) {

    deviceState = st;
    this.secret = Arrays.copyOf(secret, secret.length);
    manufacturerBlock = m;
    ownerBlock = o;
  }

  static DeviceCredentials113 of(String encoded) {
    try {
      return new DeviceCredentialsCodec().decoder().apply(CharBuffer.wrap(encoded));
    } catch (Exception e) {
      myLogger.error(e.getMessage(), e);
      return null;
    }
  }

  ManufacturerBlock getM() {
    return manufacturerBlock;
  }

  OwnerBlock getO() {
    return ownerBlock;
  }

  byte[] getSecret() {
    return Arrays.copyOf(secret, secret.length);
  }

  DeviceState getSt() {
    return deviceState;
  }

  @Override
  public Version getVersion() {
    return Version.VERSION_1_13;
  }

  @Override
  public MessageType getType() {
    return MessageType.PM_DEVICE_CREDENTIALS;
  }

  @Override
  public String toString() {
    StringWriter w = new StringWriter();
    try {
      new DeviceCredentialsCodec().encoder().apply(w, this);
      return w.toString();
    } catch (Exception e) {
      myLogger.error(e.getMessage(), e);
      return "";
    }
  }

  @Override
  public UUID getUuid() {
    return ownerBlock.getG();
  }
}
