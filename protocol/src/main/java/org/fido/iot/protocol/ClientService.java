// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Defines a Client Services for handing messages.
 */
public abstract class ClientService extends MessagingService {

  /**
   * Gets the client initial 'Hello' message as a DispatchResult.
   *
   * @return A DispatchResult that contains the initial 'Hello' Message.
   */
  public abstract DispatchResult getHelloMessage();

}
