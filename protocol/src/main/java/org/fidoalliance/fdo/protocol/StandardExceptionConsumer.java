// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.ExceptionConsumer;

public class StandardExceptionConsumer implements ExceptionConsumer {


  @Override
  public void accept(Throwable throwable) throws IOException {
    //does nothing for security
  }
}
