package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.RendezvousAcceptFunction;
import org.fidoalliance.fdo.protocol.message.To0OwnerSign;

public class StandardRendezvousAcceptFunction implements RendezvousAcceptFunction {

  @Override
  public Boolean apply(To0OwnerSign to0OwnerSign) throws IOException {

    //todo: verify device certificate chain hash
    //todo: verify device certificates revocations




    return false;
  }
}
