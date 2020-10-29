// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

/**
 * Storage notification events.
 */
public interface StorageEvents {

  /**
   * The first protocol message is about to be processed.
   *
   * @param request The message request.
   * @param reply   The message reply.
   */
  default void starting(Composite request, Composite reply) {}

  /**
   * The first protocol message has been processed.
   *
   * @param request The message request.
   * @param reply   The message reply.
   */
  default void started(Composite request, Composite reply) {}

  /**
   * A continuing protocol message about to be processed.
   *
   * @param request The message request.
   * @param reply   The message reply.
   */
  default void continuing(Composite request, Composite reply) {}

  /**
   * A continuing protocol message has been processed.
   *
   * @param request The message request.
   * @param reply   The message reply.
   */
  default void continued(Composite request, Composite reply) {}

  /**
   * The last protocol message has been processed.
   *
   * @param request The message request.
   * @param reply   The message reply.
   */
  default void completed(Composite request, Composite reply) {}

  /**
   * A protocol error has been receeved.
   *
   * @param request The message request.
   * @param reply   The message reply.
   */
  default void failed(Composite request, Composite reply) {}
}
