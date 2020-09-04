// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Message Service Interface.
 */
public abstract class MessagingService {

  protected MessagingService() {
  }

  /**
   * Gets the crypto service.
   * @return The crypto service.
   */
  public abstract CryptoService getCryptoService();

  /**
   * Dispatches a message and generates the reply.
   * @param request The incoming message request.
   * @param reply The outgoing reply message.
   * @return True if the dispatching is done, otherwise false.
   */
  public abstract boolean dispatch(Composite request, Composite reply);

}
