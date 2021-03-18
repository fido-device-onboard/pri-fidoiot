// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.serviceinfo;

import java.util.UUID;
import org.fido.iot.protocol.Composite;

/**
 * A module that's capable of sending and receiving serviceinfo messages.
 */
public interface Module {

  /**
   * Gets the Module name.
   *
   * @return The name of the module.
   */
  String getName();

  /**
   * Prepares the module state for operating on a specific GUID.
   *
   * @param guid The device supplied guid.
   */
  void prepare(UUID guid);

  /**
   * The maximum size of serviceinfo the owner can send.
   *
   * @param mtu The maximum transmission unit to use.
   */
  void setMtu(int mtu);

  /**
   * Sets the state of the module from a previously saved state.
   *
   * @param state A previously saved state.
   */
  void setState(Composite state);

  /**
   * Gets the state of the module.
   *
   * @return The current state to be saved.
   */
  Composite getState();

  /**
   * Receives service info sent by a device or owner.
   *
   * @param kvPair A composite array where the first entry is the name and the second is the value.
   * @param isMore True if more messages are to follow.
   */
  void setServiceInfo(Composite kvPair, boolean isMore);

  /**
   * Checks if the module has a serviceinfo message to send.
   *
   * @return True if the module has a message to send, otherwise false.
   */
  boolean isMore();

  /**
   * Checks if the module is done sending serviceinfo.
   *
   * @return True if the module is done, otherwise false.
   */
  boolean isDone();

  /**
   * Gets the next message name value key pair.
   *
   * @return A composite array where the first entry is the name and the second is the value.
   */
  public Composite nextMessage();


}
