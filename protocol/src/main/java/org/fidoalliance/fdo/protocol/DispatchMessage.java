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
import org.fidoalliance.fdo.protocol.message.StreamMessage;


public class DispatchMessage {

  private byte[] message;
  private ProtocolVersion protocolVersion;
  private MsgType msgType;
  private String authToken;

  public Optional<String> getAuthToken() {
    return Optional.ofNullable(authToken);
  }

  public MsgType getMsgType() {
    return msgType;
  }

  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  public byte[] getMessage() {
    return message;
  }

  public <T> T getMessage(final Class<T> clazz) throws IOException {
    try {
      return Mapper.INSTANCE.readValue(message, clazz);
    } catch (IOException e) {
      throw new MessageBodyException(e);

    }
  }

  public void setMessage(byte[] message) {
    this.message = message;
  }

  public void setMessage(AnyType anyType) throws IOException {
    message = Mapper.INSTANCE.writeValue(anyType);
  }

  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public void setMsgType(MsgType msgType) {
    this.msgType = msgType;
  }

  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

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

  public static DispatchMessage fromThrowable(Throwable throwable, DispatchMessage prevMsg) {
    DispatchMessage resultMsg = new DispatchMessage();

    ErrorMessage error = new ErrorMessage();

    error.setTimestamp(Instant.now().getEpochSecond());

    error.setCorrelationId(Config.getWorker(CryptoService.class)
        .getSecureRandom().nextLong() & 0xffffffffL);
    error.setErrorCode(ErrorCode.INTERNAL_SERVER_ERROR);
    error.setPrevMsgId(prevMsg.getMsgType());
    error.setErrorString("Unspecified error occurred.");
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
    }

    try {
      resultMsg.setMessage(AnyType.fromObject(error));
    } catch (IOException e) {
      resultMsg.setMessage(new byte[0]);
    }

    return resultMsg;
  }

}
