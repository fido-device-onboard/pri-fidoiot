// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * The common interface for all SDO protocol messages.
 */
public interface ProtocolMessage {

  /**
   * Gets the message's version.
   *
   * <p>The message's version is the version of the SDO protocol in which it is defined.
   */
  Version getVersion();

  /**
   * Gets the message's type code.
   *
   * <p>All SDO messages have a unique type ID assigned.
   */
  MessageType getType();

  /**
   * Gets the encoded message body.
   */
  default String getBody() {
    throw new UnsupportedOperationException();
  }
}
