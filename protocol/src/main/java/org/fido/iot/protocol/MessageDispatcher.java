// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;

/**
 * Manages the resources for processing protocol messages.
 */
public abstract class MessageDispatcher implements Closeable {

  protected MessageDispatcher() {
  }

  /**
   * Gets the timeout for dispatched threads.
   *
   * @return timeout in milliseconds.
   */
  public int getTimeout() {
    return 0;
  }

  /**
   * Gets the Correlation id for errors generated during dispatch.
   *
   * @return Unique number
   */
  protected long getCorrelationId() {
    return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
  }

  protected abstract MessagingService getMessagingService(Composite request);

  protected void dispatching(Composite request) {
  }

  protected void replied(Composite reply) {
  }

  protected void failed(Exception e) {
  }

  /**
   * Reads a transport stream message from channel.
   *
   * @param channel The channel to read from.
   * @return Returns the transport stream message.
   * @throws TransportException If error occurs reading message from the channel.
   */
  public Composite readMessage(ReadableByteChannel channel) {

    //read first 4 bytes of the message from the channel
    ByteBuffer leadBytes = ByteBuffer.allocate(Const.MIN_READ_SIZE);

    try {

      while (leadBytes.hasRemaining()) {
        int r = channel.read(leadBytes);
        if (r < 0) {
          break;
        }
      }

      //must be able to read 4 bytes
      if (leadBytes.hasRemaining()) {
        throw new TransportException(new MessageBodyException());
      }

      leadBytes.flip();

      //first byte must indicate array of 5
      if (leadBytes.get() != Const.ARRAY5_TOKEN) {
        throw new TransportException(new MessageBodyException());
      }

      //trick the parse to give just first element
      ByteBuffer buffer = ByteBuffer.allocate(leadBytes.capacity());
      buffer.put((byte) Const.ARRAY1_TOKEN);
      buffer.put(leadBytes);
      buffer.flip();
      Composite msg = Composite.fromObject(buffer);

      int msgLen = msg.getAsNumber(Const.SM_LENGTH).intValue();

      if (msgLen > Const.MAX_READ_SIZE) {
        throw new TransportException(new MessageBodyException());
      }

      //now create the total buffer
      buffer = ByteBuffer.allocate(msgLen);

      //copy the original 4 lead bytes
      leadBytes.rewind();
      buffer.put(leadBytes);

      //read the remaining bytes
      while (buffer.hasRemaining()) {
        int r = channel.read(buffer);
        if (r < 0) {
          break;
        }
      }

      buffer.flip();
      byte[] cc = Composite.unwrap(buffer);
      msg = Composite.fromObject(cc);

      //now verify structure
      msg.getAsNumber(Const.SM_MSG_ID);
      msg.getAsNumber(Const.SM_PROTOCOL_VERSION);
      msg.getAsMap(Const.SM_PROTOCOL_INFO);
      msg.get(Const.SM_BODY);

      return msg;

    } catch (Exception e) {
      throw new TransportException(e);
    }
  }

  /**
   * Dispatches a message.
   *
   * @param request The message to dispatch.
   * @return The result of the dispatched message.
   */
  public DispatchResult dispatch(Composite request) {

    try {

      dispatching(request);

      Composite reply = Composite.newArray()
          .set(Const.SM_LENGTH, Const.DEFAULT)
          .set(Const.SM_MSG_ID, Const.DEFAULT)
          .set(Const.SM_PROTOCOL_VERSION, request.get(Const.SM_PROTOCOL_VERSION))
          .set(Const.SM_PROTOCOL_INFO, request.get(Const.SM_PROTOCOL_INFO))
          .set(Const.SM_BODY, Const.EMPTY_MESSAGE);

      if (request.getAsNumber(Const.SM_MSG_ID).intValue() == Const.ERROR) {

        Composite body = request.getAsComposite(Const.SM_BODY);
        int prevMessage = body.getAsNumber(Const.EM_PREV_MSG_ID).intValue();

        Composite errorRequest = Composite.newArray()
            .set(Const.SM_LENGTH, Const.DEFAULT)
            .set(Const.SM_MSG_ID, prevMessage)
            .set(Const.SM_PROTOCOL_VERSION, request.get(Const.SM_PROTOCOL_VERSION))
            .set(Const.SM_PROTOCOL_INFO, request.get(Const.SM_PROTOCOL_INFO))
            .set(Const.SM_BODY, Const.EMPTY_MESSAGE);

        MessagingService service = getMessagingService(errorRequest);

        service.dispatch(request, reply);

        return new DispatchResult(Const.EMPTY_MESSAGE, true);
      }

      MessagingService service = getMessagingService(request);

      boolean isDone = service.dispatch(request, reply);

      DispatchResult dr = new DispatchResult(reply, isDone);
      replied(reply);
      return dr;
    } catch (Exception e) {
      failed(e);
      DispatchResult dr = getDispatchError(request, e);
      replied(dr.getReply());
      return dr;
    }
  }

  /**
   * Writes a message to a channel.
   *
   * @param client The channel client.
   * @param msg    The message to write.
   */
  public void writeMessage(WritableByteChannel client, Composite msg) {
    try {
      ByteBuffer buf = msg.toByteBuffer();
      while (buf.hasRemaining()) {
        client.write(buf);
      }
    } catch (Exception e) {
      throw new TransportException();
    }
  }

  protected DispatchResult getDispatchError(Composite request, Exception e) {

    DispatchException dispatchException = null;
    Throwable causeException = e;
    StringBuilder msgBuilder = new StringBuilder("");

    while (causeException != null) {
      String msg = causeException.getMessage();

      if (msgBuilder.length() > 0) {
        msgBuilder.append(Const.ERROR_CAUSE);
      }
      if (msg == null) {
        msg = causeException.getClass().getSimpleName();
      }
      msgBuilder.append(msg);
      if (causeException instanceof DispatchException) {
        dispatchException = (DispatchException) causeException;
        break;
      }
      causeException = causeException.getCause();
    }

    if (msgBuilder.length() > Const.ERROR_MAX_LENGTH) {
      msgBuilder.setLength(Const.ERROR_MAX_LENGTH);
    }

    if (dispatchException == null) {
      dispatchException = new DispatchException(e);
    }

    Composite reply = dispatchException.getError(request);

    Composite body = reply.getAsComposite(Const.SM_BODY);
    body.set(Const.EM_ERROR_UUID, getCorrelationId());
    body.set(Const.EM_ERROR_STR, msgBuilder.toString());
    return new DispatchResult(reply, true);
  }

  @Override
  public void close() throws IOException {

  }
}
