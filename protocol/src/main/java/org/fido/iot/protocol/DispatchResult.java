// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import org.fido.iot.protocol.cbor.Encoder;

/**
 * The result of dispatching a Message.
 */
public class DispatchResult {

  private final Composite reply;
  private final boolean done;

  /**
   * Constructs a Dispatch Result.
   *
   * @param reply The reply message.
   * @param done  True if this is the last message, otherwise false.
   */
  public DispatchResult(Composite reply, boolean done) {
    this.reply = reply;
    calculateLength(reply);
    if (reply.size() > 0 && reply.getAsNumber(Const.SM_MSG_ID).intValue() == Const.ERROR) {
      this.done = true;
    } else {
      this.done = done;
    }
  }

  /**
   * Determines if the message has non-empty reply.
   *
   * @return True if the message has a non-empty reply, otherwise false.
   */
  public boolean hasReply() {
    return reply.size() > 0;
  }

  public Composite getReply() {
    return reply;
  }

  /**
   * Determines if this is the last message.
   *
   * @return True if this is the last message, otherwise false.
   */
  public boolean isDone() {
    return done;
  }

  /**
   * Calculates the cbor message length in bytes.
   *
   * @param msg The message to calculate.
   * @return The size of the message in bytes.
   */
  public int calculateLength(Composite msg) {

    if (msg.size() > 0) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      WritableByteChannel wbc = Channels.newChannel(out);
      Encoder encoder = new Encoder.Builder(wbc).build();
      try {
        encoder.writeObject(msg.get());
        do {
          msg.set(Const.SM_LENGTH, out.size());
          out.reset();
          encoder.writeObject(msg.get());
        } while (msg.getAsNumber(Const.SM_LENGTH).intValue() != out.size());

        return msg.getAsNumber(Const.SM_LENGTH).intValue();
      } catch (IOException e) {
        throw new TransportException(e);
      }
    }
    return 0;
  }

}
