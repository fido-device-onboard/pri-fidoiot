// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.message.ServiceInfoKeyValuePair;
import org.fidoalliance.fdo.protocol.message.ServiceInfoModuleState;

/**
 * ServiceInfo Module worker.
 */
public interface ServiceInfoModule {

  /**
   * Gets the Name of the Module.
   *
   * @return The service info module Name.
   */
  String getName();

  /**
   * Prepares the ServiceInfo Module for sending message.
   *
   * @param state The stored state of the serviceInfo module
   * @throws IOException An error occurred.
   */
  void prepare(ServiceInfoModuleState state) throws IOException;

  /**
   * Receives service info values.
   *
   * @param state  The stored state of the serviceInfo module.
   * @param kvPair The ServiceInfoKeyValuePair to receive.
   * @throws IOException An error occurred.
   */
  void receive(ServiceInfoModuleState state, ServiceInfoKeyValuePair kvPair) throws IOException;

  /**
   * Called when empty service info message received.
   *
   * @throws IOException An error occurred.
   */
  void keepAlive() throws IOException;


  /**
   * Sends service info values.
   *
   * @param state        The stored state of the serviceInfo module.
   * @param sendFunction The function used to send values.
   * @throws IOException An error occurred.
   */
  void send(ServiceInfoModuleState state, ServiceInfoSendFunction sendFunction) throws IOException;
}
