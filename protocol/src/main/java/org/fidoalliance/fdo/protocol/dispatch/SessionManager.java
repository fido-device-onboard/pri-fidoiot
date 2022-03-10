// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;

import org.fidoalliance.fdo.protocol.message.SimpleStorage;

/**
 * A Manages Storage sessions.
 */
public interface SessionManager {

  /**
   * Gets an existing Session Object.
   * @param name The unique name of the Storage Session Object
   * @return The session object.
   */
  SimpleStorage getSession(String name) throws IOException;

  /**
   * Saves a new session.
   * @param name The name of the new session
   * @param storage A simple storage Object.
   */
  void saveSession(String name,SimpleStorage storage) throws IOException;

  /**
   * Updates an existing session.
   * @param name The name of the new session.
   * @param storage A simple storage Object.
   */
  void updateSession(String name,SimpleStorage storage) throws IOException;


  /**
   * Marks a session as expired.
   * @param name The name of the session to expire
   */
  void expireSession(String name);

}
