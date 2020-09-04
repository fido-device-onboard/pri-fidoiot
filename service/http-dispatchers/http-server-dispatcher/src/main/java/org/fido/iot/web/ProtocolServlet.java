// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.web;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.MessageDispatcher;

/**
 * A HTTP Servlet for process HTTP messages.
 */
public class ProtocolServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    MessageDispatcher manager = (MessageDispatcher) getServletContext()
        .getAttribute(Const.DISPATCHER_ATTRIBUTE);
    AsyncContext asyncCtx = req.startAsync();
    asyncCtx.setTimeout(manager.getTimeout());
    ProtocolHandler handler = new ProtocolHandler(manager, asyncCtx);
    asyncCtx.start(handler);
  }

}
