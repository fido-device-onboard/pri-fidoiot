// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.DispatchResult;
import org.fido.iot.protocol.MessageDispatcher;

/**
 * Base class for all message Handlers.
 */
public class ProtocolHandler implements Runnable {

  private MessageDispatcher dispatcher;
  private AsyncContext asyncCtx;

  /**
   * Constructs ProtocolHandler instance.
   *
   * @param dispatcher A message dispatcher.
   * @param asyncCtx   A servlet AsyncContext.
   */
  public ProtocolHandler(MessageDispatcher dispatcher, AsyncContext asyncCtx) {
    this.dispatcher = dispatcher;
    this.asyncCtx = asyncCtx;
  }

  private void writeException(HttpServletResponse response, Exception e) {
    try {
      OutputStream out = response.getOutputStream();
      try (PrintStream s = new PrintStream(out, true, StandardCharsets.US_ASCII.name())) {
        response.setContentType("text/plain; charset=us-ascii");
        e.printStackTrace(s);
      }
    } catch (UnsupportedEncodingException uee) {
      //nothing we can due since we are already processing an Exception
    } catch (IOException ioException) {
      //nothing we can due since we are already processing an Exception
    }
  }

  private Composite getMessage(HttpServletRequest request) throws IOException {
    String bearer;
    Composite token = Composite.newMap();

    String[] parts = request.getRequestURI().split("/");
    int index = parts.length - 1;

    int msgId = Integer.parseInt(parts[index--]);
    index--; //skip msg index
    int version = Integer.parseInt(parts[index--]);

    //get Authorization token
    Enumeration<String> values = request.getHeaders(Const.HTTP_AUTHORIZATION);
    while (values.hasMoreElements()) {
      String authHeader = values.nextElement();
      parts = authHeader.split(" ");
      if (parts.length > 0) {
        if (parts[0].compareToIgnoreCase(Const.HTTP_BEARER) == 0) {
          token.set(Const.PI_TOKEN, parts[1]);
        }
      }
    }

    byte[] body = request.getInputStream().readNBytes(request.getContentLength());
    return Composite.newArray()
        .set(Const.SM_LENGTH, request.getContentLength())
        .set(Const.SM_PROTOCOL_VERSION, version)
        .set(Const.SM_MSG_ID, msgId)
        .set(Const.SM_PROTOCOL_INFO, token)
        .set(Const.SM_BODY, Composite.fromObject(body));

  }

  @Override
  public void run() {
    HttpServletRequest request = (HttpServletRequest) asyncCtx.getRequest();
    HttpServletResponse response = (HttpServletResponse) asyncCtx.getResponse();

    if (request.getContentType() == null
        || request.getContentType().compareToIgnoreCase(Const.HTTP_APPLICATION_CBOR) != 0) {
      response.setStatus(Const.HTTP_UNSUPPORTED_MEDIA_TYPE);
      return;
    }

    if (request.getContentLength() < 0) {
      response.setStatus(Const.HTTP_LENGTH_REQUIRED);
      return;
    }

    try {
      Composite msg = getMessage(request);
      DispatchResult result = dispatcher.dispatch(msg);
      Composite reply = result.getReply();
      int replyId = reply.getAsNumber(Const.SM_MSG_ID).intValue();
      if (replyId == Const.ERROR) {
        response.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
      } else {
        response.setHeader(Const.HTTP_MESSAGE_TYPE,
            Integer.toUnsignedString(
                reply.getAsNumber(Const.SM_MSG_ID)
                    .intValue()));
      }

      //set content type to cbor
      response.setContentType(Const.HTTP_APPLICATION_CBOR);

      //set bearer token (if any)
      Composite info = reply.getAsComposite(Const.SM_PROTOCOL_INFO);
      if (info.containsKey(Const.PI_TOKEN)) {
        response.setHeader(Const.HTTP_AUTHORIZATION,
            Const.HTTP_BEARER + " " + info.getAsString(Const.PI_TOKEN));
      }

      //send the body
      byte[] body = reply.getAsComposite(Const.SM_BODY).toBytes();
      response.setContentLength(body.length);
      response.getOutputStream().write(body);
    } catch (IOException e) {
      response.setStatus(Const.HTTP_INTERNAL_SERVER_ERROR);
      writeException(response, e);
    } finally {
      asyncCtx.complete();
    }
  }
}
