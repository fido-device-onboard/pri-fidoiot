// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.RendezvousAcceptFunction;
import org.fidoalliance.fdo.protocol.message.To0OwnerSign;

public class UntrustedRendezvousAcceptFunction implements RendezvousAcceptFunction {

  @Override
  public Boolean apply(To0OwnerSign to0OwnerSign) throws IOException {
    return true;
  }
}
