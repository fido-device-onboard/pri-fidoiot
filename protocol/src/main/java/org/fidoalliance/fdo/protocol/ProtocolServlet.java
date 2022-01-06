package org.fidoalliance.fdo.protocol;


import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;
import org.fidoalliance.fdo.protocol.dispatch.MessageDispatcher;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.ErrorMessage;
import org.fidoalliance.fdo.protocol.message.MsgType;
import org.fidoalliance.fdo.protocol.message.ProtocolInfo;
import org.fidoalliance.fdo.protocol.message.StreamMessage;

public class ProtocolServlet extends HttpServlet {


  private static LoggerService logger = new LoggerService(ProtocolServlet.class);

  protected void logMessage(DispatchMessage msg) {
    StringBuilder builder = new StringBuilder();
    builder.append("Type ");
    builder.append(msg.getMsgType().toInteger());
    builder.append(" ");
    try {
      Mapper.INSTANCE.writeDiagnostic(builder,
          Mapper.INSTANCE.readValue(msg.getMessage(), AnyType.class));
    } catch (Exception e)  {
      builder.append("failed to covert to diagnostic form.");
    }

    logger.info(builder.toString());
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {

    DispatchMessage reqMsg = null;
    try {
      reqMsg = HttpUtils.getMessageFromURI(req.getRequestURI());

      Enumeration<String> values = req.getHeaders(HttpUtils.HTTP_AUTHORIZATION);
      while (values.hasMoreElements()) {
        reqMsg.setAuthToken(values.nextElement());
      }

      if (req.getContentLength() > BufferUtils.getMaxBufferSize()) {
        throw new MessageBodyException("message too large.");
      }

      reqMsg.setMessage(req.getInputStream().readNBytes(req.getContentLength()));

      logMessage(reqMsg);

      MessageDispatcher dispatcher = Config.getWorker(StandardMessageDispatcher.class);
      Optional<DispatchMessage> result = dispatcher.dispatch(reqMsg);

      if (result.isPresent()) {
        DispatchMessage respMsg = result.get();
        if (respMsg.getMsgType() == MsgType.ERROR) {
          resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        resp.setHeader(HttpUtils.HTTP_AUTHORIZATION,respMsg.getAuthToken().get());
        resp.setContentType(HttpUtils.HTTP_APPLICATION_CBOR);
        resp.setHeader(HttpUtils.HTTP_MESSAGE_TYPE,
            Integer.toString(respMsg.getMsgType().toInteger()));
        resp.setContentLength(respMsg.getMessage().length);
        resp.getOutputStream().write(respMsg.getMessage());

        logMessage(respMsg);
      }

    } catch (Throwable throwable) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      if (reqMsg != null) {
        DispatchMessage errorMsg = DispatchMessage.fromThrowable(throwable, reqMsg);

        try {
          resp.setContentLength(errorMsg.getMessage().length);
          resp.getOutputStream().write(errorMsg.getMessage());
        } catch (Throwable throwable1) {
          logger.error("failed to write error response");
          // already in exception handler
        }

        logMessage(errorMsg);
      }

      throwable.printStackTrace();
    }
  }
}
