// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fidoalliance.fdo.protocol.dispatch.ExceptionConsumer;

public class DebugExceptionConsumer implements ExceptionConsumer {

  private static final LoggerService logger = new LoggerService(DebugExceptionConsumer.class);

  public DebugExceptionConsumer() {
    logger.warn("Using DebugExceptionConsumer - this should not be used in production");
  }

  @Override
  public void accept(Throwable throwable) throws IOException {
    logger.info(ExceptionUtils.getStackTrace(throwable));

  }
}
