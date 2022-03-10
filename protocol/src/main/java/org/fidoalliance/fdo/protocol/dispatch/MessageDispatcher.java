// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import java.util.Optional;
import org.fidoalliance.fdo.protocol.DispatchMessage;
import org.fidoalliance.fdo.protocol.message.StreamMessage;

/**
 * Dispatches protocol messages.
 */
public interface MessageDispatcher {

  /**
   * Dispatches a message.
   * @param request The request message.
   * @return The response if any.
   */
  Optional<DispatchMessage> dispatch(DispatchMessage request) throws IOException;
}
