// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.fidoalliance.fdo.protocol.dispatch.CryptoService;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.ErrorCode;
import org.fidoalliance.fdo.protocol.message.ErrorMessage;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolInfo;
import org.fidoalliance.fdo.protocol.message.ProtocolVersion;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;
import org.fidoalliance.fdo.protocol.message.StreamMessage;


public class DispatchMessage {

  private byte[] message;
  private ProtocolVersion protocolVersion;
  private MsgType msgType;
  private String authToken;
  private SimpleStorage extra;

  private static void findCause(ErrorMessage error, Throwable throwable) {
  }

  /**
   * Gets the authorization token that sent the cbor message.
   * @return The authorization token.
   */
  public Optional<String> getAuthToken() {
    return Optional.ofNullable(authToken);
  }

  /**
   * Gets the Message Type of the cbor message.
   * @return The Message type.
   */
  public MsgType getMsgType() {
    return msgType;
  }

  /**
   * Gets the protocol version of the cbor message.
   * @return The protocol version.
   */
  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  /**
   * Gets the cbor message.
   * @return The cbor bytes of the message.
   */
  public byte[] getMessage() {
    return message;
  }

  /**
   * Coverts the cbor message to a Java object.
   * @param clazz The class to covert the cbor message too.
   * @param <T> The template argument.
   * @return The Java object the represents the cbor message
   * @throws IOException An error occurred.
   */
  public <T> T getMessage(final Class<T> clazz) throws IOException {
    try {
      return Mapper.INSTANCE.readValue(message, clazz);
    } catch (IOException e) {
      throw new MessageBodyException(e);

    }
  }

  /**
   * Gets the extra storage associated with the message.
   * @return The extra storage.
   */
  public SimpleStorage getExtra() {
    return extra;
  }

  /**
   * Sets the cbor message.
   * @param message The cbor bytes of the message.
   */
  public void setMessage(byte[] message) {
    this.message = message;
  }


  /**
   * Sets the protocol version of the message.
   * @param protocolVersion The protocol version.
   */
  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  /**
   * Sets the message type.
   * @param msgType The message type.
   */
  public void setMsgType(MsgType msgType) {
    this.msgType = msgType;
  }

  /**
   * Sets the Authorization token.
   * @param authToken The Authorization token.
   */
  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  /**
   * Sets the extra storage associated with the message.
   * @param extra The extra storage.
   */
  public void setExtra(SimpleStorage extra) {
    this.extra = extra;
  }

  /**
   * Converts the message to a stream message for non HTTP protocols.
   * @return A stream message.
   * @throws IOException An error occurred.
   */
  public StreamMessage toStreamMessage() throws IOException {
    StreamMessage streamMessage = new StreamMessage();
    ProtocolInfo info = ProtocolInfo.empty();
    if (authToken != null) {
      info = new ProtocolInfo();
      info.setAuthToken(authToken);
    }
    streamMessage.setProtocolInfo(info);
    streamMessage.setProtocolVersion(protocolVersion);
    streamMessage.setMsgType(msgType);
    streamMessage.setBody(AnyType.fromObject(Mapper.INSTANCE.readTree(message)));
    streamMessage.computeLength();

    return streamMessage;
  }

  /**
   * Gets a cbor 255 error message from a thrown exception.
   * @param throwable The exception that was thrown.
   * @param prevMsg The message that was being processed when the exception occurred.
   * @return The 255 error message.
   */
  public static DispatchMessage fromThrowable(Throwable throwable, DispatchMessage prevMsg) {


    ErrorMessage error = new ErrorMessage();

    error.setTimestamp(Instant.now().getEpochSecond());

    error.setCorrelationId(Config.getWorker(CryptoService.class)
        .getSecureRandom().nextLong() & 0xffffffffL);
    error.setErrorCode(ErrorCode.INTERNAL_SERVER_ERROR);
    error.setPrevMsgId(prevMsg.getMsgType());
    error.setErrorString("Unspecified error occurred.");

    DispatchMessage resultMsg = new DispatchMessage();
    resultMsg.setMsgType(MsgType.ERROR);

    Throwable current = throwable;
    DispatchException found = null;
    while (current != null) {
      if (current instanceof DispatchException) {
        found = (DispatchException) current;
      }
      current = current.getCause();
    }
    if (found != null) {
      error.setErrorCode(found.getErrorCode());
      error.setErrorString(found.getMessage());
    } else {
      findCause(error,throwable);
    }

    try {
      resultMsg.setMessage(Mapper.INSTANCE.writeValue(error));
    } catch (IOException e) {
      resultMsg.setMessage(new byte[0]);
    }

    return resultMsg;
  }

}
