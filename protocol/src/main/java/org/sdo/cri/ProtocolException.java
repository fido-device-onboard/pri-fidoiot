// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.io.IOException;
import java.io.StringWriter;

/**
 * An exception which signals a failure of the onboarding protocol.
 */
public class ProtocolException extends RuntimeException {

  private Error error;

  public ProtocolException(Error err) {
    super(buildMessage(err));
    setError(err);
  }

  public ProtocolException(Error err, Throwable cause) {
    super(buildMessage(err), cause);
    setError(err);
  }

  private static String buildMessage(Error err) {

    try {
      StringWriter writer = new StringWriter();
      new ErrorCodec().encoder().apply(writer, err);
      return writer.toString();

    } catch (IOException e) {
      return err.getEm();
    }
  }

  public Error getError() {
    return error;
  }

  private void setError(Error err) {
    error = err;
  }
}
