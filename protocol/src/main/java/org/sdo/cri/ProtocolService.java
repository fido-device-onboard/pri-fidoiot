// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

/**
 * A Secure Device Onboard service.
 *
 * <p>Services respond to ProtocolMessages one at a time until completion.
 */
public interface ProtocolService {

  /**
   * Return true if the protocol is complete and will accept no more inputs.
   */
  boolean isDone();

  /**
   * Return true if the given message is a 'hello' initialization for this service.
   */
  boolean isHello(ProtocolMessage message);

  /**
   * Handle the next input message in the protocol.
   *
   * @param in The input request
   * @return The output response
   * @throws ProtocolException If the protocol fails
   */
  ProtocolMessage next(ProtocolMessage in) throws ProtocolException;
}
