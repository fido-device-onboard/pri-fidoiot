package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.CredReuseFunction;

public class StandardCredReuseFunction implements CredReuseFunction {

  @Override
  public Boolean apply(Boolean value) throws IOException {
    return true;
  }
}
